package bd.fasol.auth.controller;

import bd.fasol.auth.dto.request.LoginRequest;
import bd.fasol.auth.dto.request.RegisterRequest;
import bd.fasol.auth.dto.request.RefreshTokenRequest;
import bd.fasol.auth.dto.response.AuthResponse;
import bd.fasol.auth.service.AuthService;
import bd.fasol.model.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request, Authentication authentication) {
        User actor = authentication == null ? null : (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request, actor));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return service.refresh(request);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        service.logout(request);
        return ResponseEntity.ok().build();
    }
}
