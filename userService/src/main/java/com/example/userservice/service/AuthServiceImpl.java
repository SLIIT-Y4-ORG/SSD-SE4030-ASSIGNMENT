package com.example.userservice.service;

import com.example.userservice.dto.TokenValidationResponse;
import com.example.userservice.model.*;
import com.example.userservice.exception.AuthRegistrationException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.PasswordService;
import com.example.userservice.security.TokenService;
import com.example.userservice.security.TokenService.TokenType;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;

    public AuthServiceImpl(UserRepository userRepository, PasswordService passwordService, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    @Override
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new AuthRegistrationException("User with email already exists");
        }
        
        // Public registration must never accept identity or privilege fields from the client.
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.PATIENT);
        user.setPassword(passwordService.hash(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public AuthResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new AuthRegistrationException("Invalid credentials");
        }

        String token = tokenService.issue(user.getId(), TokenType.ACCESS);
        String refreshToken = tokenService.issue(user.getId(), TokenType.REFRESH);
        
        AuthResponse response = new AuthResponse();
        response.setAccessToken(token);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        response.setUser(user);
        
        return response;
    }

    @Override
    public void logoutUser(String accessToken) {
        // Simple logout - in real app, invalidate token
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        UUID userId = tokenService.verify(refreshToken, TokenType.REFRESH);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        AuthResponse response = new AuthResponse();
        response.setAccessToken(tokenService.issue(userId, TokenType.ACCESS));
        response.setRefreshToken(tokenService.issue(userId, TokenType.REFRESH));
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        response.setUser(user);
        return response;
    }

    @Override
    public User getCurrentUser(String accessToken) {
        UUID userId = tokenService.verify(accessToken, TokenType.ACCESS);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public TokenValidationResponse validateToken(String accessToken) {
        try {
            User user = getCurrentUser(accessToken);
            
            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("Invalid token")
                    .build();
        }
    }
}
