package com.example.userservice.service;

import com.example.userservice.dto.TokenValidationResponse;
import com.example.userservice.model.*;
import com.example.userservice.exception.AuthRegistrationException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new AuthRegistrationException("User with email already exists");
        }
        
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        
        return userRepository.save(user);
    }

    @Override
    public AuthResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Simple password check (in real app, you'd hash/verify properly)
        if (!request.getPassword().equals(user.getPassword())) {
            throw new AuthRegistrationException("Invalid credentials");
        }
        
        // Generate simple token (in real app, use JWT)
        String token = "token_" + user.getId().toString();
        
        AuthResponse response = new AuthResponse();
        response.setAccessToken(token);
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
        // Simple refresh - return same token
        AuthResponse response = new AuthResponse();
        response.setAccessToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        
        return response;
    }

    @Override
    public User getCurrentUser(String accessToken) {
        // Extract user ID from simple token format
        if (accessToken.startsWith("token_")) {
            String userIdStr = accessToken.substring(6);
            UUID userId = UUID.fromString(userIdStr);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
        }
        throw new AuthRegistrationException("Invalid token");
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