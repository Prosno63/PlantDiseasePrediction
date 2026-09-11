package bd.fasol.common.security;

import bd.fasol.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long minutes;
    private final long refreshDays;

    public JwtService(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long minutes,
            @Value("${jwt.refresh-expiration-days}") long refreshDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.minutes = minutes;
        this.refreshDays = refreshDays;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.id.toString())
                .claim("role", user.role.name())
                .claim("phone", user.phoneNumber)
                .claim("name", user.name)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(minutes * 60)))
                .signWith(key)
                .compact();
    }

    public long expiresInSeconds() {
        return minutes * 60;
    }

    public long refreshExpiresInSeconds() {
        return refreshDays * 24 * 60 * 60;
    }

    public Long userId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String role(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
