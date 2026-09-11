package bd.fasol.auth.service;

import bd.fasol.auth.dto.request.LoginRequest;
import bd.fasol.auth.dto.request.RefreshTokenRequest;
import bd.fasol.auth.dto.request.RegisterRequest;
import bd.fasol.auth.dto.response.AuthResponse;
import bd.fasol.auth.dto.response.UserResponse;
import bd.fasol.common.exception.ApiException;
import bd.fasol.common.security.JwtService;
import bd.fasol.model.Role;
import bd.fasol.model.RefreshToken;
import bd.fasol.model.User;
import bd.fasol.repository.RefreshTokenRepository;
import bd.fasol.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AuthService {
    private final UserRepository users;
    private final BCryptPasswordEncoder passwords;
    private final JwtService jwt;
    private final RefreshTokenRepository refreshTokens;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, BCryptPasswordEncoder passwords, JwtService jwt,
            RefreshTokenRepository refreshTokens) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
    }

    public BCryptPasswordEncoder getPasswordEncoder() {
        return passwords;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, User actor) {
        return response(createUser(request, actor));
    }

    @Transactional
    public User createUser(RegisterRequest request, User actor) {
        Role role = request.role() == null ? Role.FARMER : request.role();
        if (role == Role.ADMIN && (actor == null || actor.role != Role.ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only admins can create admin accounts");
        }
        if (users.findByPhoneNumber(request.phoneNumber()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone number already registered");
        }
        return users.save(newUser(request, role));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "Invalid phone number or password"));
        if (user.passwordHash == null || !passwords.matches(request.password(), user.passwordHash)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid phone number or password");
        }
        return response(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokens.findByTokenHashAndRevokedFalse(hash(request.refreshToken()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (refreshToken.expiresAt.isBefore(Instant.now())) {
            refreshToken.revoked = true;
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        refreshToken.revoked = true;
        return response(refreshToken.user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request, User actor) {
        refreshTokens.findByTokenHashAndRevokedFalse(hash(request.refreshToken())).ifPresent(token -> {
            if (!token.user.id.equals(actor.id)) return;
            token.revoked = true;
            refreshTokens.save(token);
        });
    }

    private User newUser(RegisterRequest request, Role role) {
        User user = new User();
        user.phoneNumber = request.phoneNumber();
        user.passwordHash = passwords.encode(request.password());
        user.name = request.name();
        user.role = role;
        user.profileImageUrl = request.profileImageUrl();
        user.designation = request.designation();
        user.qualification = request.qualification();
        user.specialization = request.specialization();
        if (request.available() != null) user.available = request.available();
        if (request.online() != null) user.online = request.online();
        return user;
    }

    private AuthResponse response(User user) {
        RefreshToken refreshToken = new RefreshToken();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        refreshToken.tokenHash = hash(rawToken);
        refreshToken.user = user;
        refreshToken.expiresAt = Instant.now().plusSeconds(jwt.refreshExpiresInSeconds());
        refreshTokens.save(refreshToken);
        return new AuthResponse(
                jwt.issue(user), rawToken, "Bearer", jwt.expiresInSeconds(), UserResponse.from(user));
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
