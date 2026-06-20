package com.incidentiq.core.controller;

import com.incidentiq.core.dto.request.LoginRequest;
import com.incidentiq.core.dto.request.RefreshRequest;
import com.incidentiq.core.dto.request.RegisterRequest;
import com.incidentiq.core.dto.response.AuthResponse;
import com.incidentiq.core.security.SecurityConstants;
import com.incidentiq.core.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        String token = null;
        if (header != null && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            token = header.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
