package com.example.userservice.service;

import com.example.userservice.dto.TokenValidationResponse;
import com.example.userservice.model.*;

public interface AuthService {
    User registerUser(User user);

    AuthResponse loginUser(LoginRequest request);

    void logoutUser(String accessToken);

    AuthResponse refreshToken(String refreshToken);

    User getCurrentUser(String accessToken);

    TokenValidationResponse validateToken(String accessToken);

    AuthResponse loginWithGoogle(String code, String redirectUri);

    String getGoogleAuthorizationUrl(String state, String redirectUri);
}
