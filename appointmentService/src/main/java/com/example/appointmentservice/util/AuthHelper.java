package com.example.appointmentservice.util;

import com.example.appointmentservice.client.UserServiceClient;
import com.example.appointmentservice.dto.TokenValidationResponse;
import com.example.appointmentservice.exception.ForbiddenException;
import com.example.appointmentservice.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AuthHelper {
    private final UserServiceClient userServiceClient;

    public AuthHelper(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    public TokenValidationResponse requireAuthenticated(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authentication required");
        }
        TokenValidationResponse auth;
        try {
            auth = userServiceClient.validateToken(authHeader);
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("Invalid token");
        }
        if (auth == null || !auth.isValid()) {
            throw new UnauthorizedException("Invalid token");
        }
        return auth;
    }

    public TokenValidationResponse requireRole(String authHeader, String... roles) {
        TokenValidationResponse auth = requireAuthenticated(authHeader);
        if (auth.getRole() == null || Arrays.stream(roles).noneMatch(auth.getRole()::equals)) {
            throw new ForbiddenException("Insufficient privileges");
        }
        return auth;
    }
}
