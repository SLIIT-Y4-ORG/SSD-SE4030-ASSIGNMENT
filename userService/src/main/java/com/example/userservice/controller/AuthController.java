package com.example.userservice.controller;

import com.example.userservice.dto.TokenValidationResponse;
import com.example.userservice.model.*;
import com.example.userservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginUser(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logoutUser(stripBearer(authHeader));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.getCurrentUser(stripBearer(authHeader)));
    }

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.validateToken(stripBearer(authHeader)));
    }

    private String stripBearer(String authHeader) {
        if (authHeader == null || authHeader.length() < 8
                || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)
                || authHeader.substring(7).isBlank()) {
            throw new com.example.userservice.exception.AuthRegistrationException("Bearer token required");
        }
        return authHeader.substring(7).trim();
    }
}
