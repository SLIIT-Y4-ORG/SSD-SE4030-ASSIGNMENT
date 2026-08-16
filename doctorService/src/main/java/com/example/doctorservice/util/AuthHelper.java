package com.example.doctorservice.util;

import com.example.doctorservice.client.UserServiceClient;
import com.example.doctorservice.dto.TokenValidationResponse;
import com.example.doctorservice.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AuthHelper {

    private final UserServiceClient userServiceClient;

    public AuthHelper(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    public void requireAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authentication required");
        }
        TokenValidationResponse auth = userServiceClient.validateToken(stripBearer(authHeader));
        if (!auth.isValid()) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    public void requireRole(String authHeader, String... roles) {
        requireAuth(authHeader);
        TokenValidationResponse auth = userServiceClient.validateToken(stripBearer(authHeader));
        if (auth.getRole() == null || Arrays.stream(roles).noneMatch(role -> role.equals(auth.getRole()))) {
            throw new UnauthorizedException("Insufficient privileges");
        }
    }

    public void requireAuthenticated(String authHeader) {
        requireAuth(authHeader);
    }

    private String stripBearer(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : header;
    }
}