package com.example.doctorservice.util;

import com.example.doctorservice.client.UserServiceClient;
import com.example.doctorservice.dto.TokenValidationResponse;
import com.example.doctorservice.exception.UnauthorizedException;
import com.example.doctorservice.exception.ForbiddenException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AuthHelper {

    private final UserServiceClient userServiceClient;

    public AuthHelper(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    public TokenValidationResponse requireAuth(String authHeader) {
        if (authHeader == null || authHeader.length() < 8
                || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)
                || authHeader.substring(7).isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
        TokenValidationResponse auth = userServiceClient.validateToken(stripBearer(authHeader));
        if (!auth.isValid() || auth.getUserId() == null) {
            throw new UnauthorizedException("Invalid token");
        }
        return auth;
    }

    public TokenValidationResponse requireRole(String authHeader, String... roles) {
        TokenValidationResponse auth = requireAuth(authHeader);
        if (auth.getRole() == null || Arrays.stream(roles).noneMatch(role -> role.equals(auth.getRole()))) {
            throw new ForbiddenException("Insufficient privileges");
        }
        return auth;
    }

    public TokenValidationResponse requireAuthenticated(String authHeader) {
        return requireAuth(authHeader);
    }

    public String stripBearer(String header) {
        return header.substring(7).trim();
    }
}
