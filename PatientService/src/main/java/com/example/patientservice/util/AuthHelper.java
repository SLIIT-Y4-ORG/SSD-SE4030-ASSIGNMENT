package com.example.patientservice.util;

import com.example.patientservice.client.UserServiceClient;
import com.example.patientservice.dto.TokenValidationResponse;
import com.example.patientservice.exception.ForbiddenException;
import com.example.patientservice.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AuthHelper {

    private final UserServiceClient userServiceClient;

    public AuthHelper(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    /**
     * Validates the token and returns the validation response.
     */
    public TokenValidationResponse requireAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authentication required");
        }
        TokenValidationResponse auth = userServiceClient.validateToken(stripBearer(authHeader));
        if (!auth.isValid()) {
            throw new UnauthorizedException("Invalid token");
        }
        return auth;
    }

    /**
     * Validates the token and ensures the user has one of the specified roles.
     */
    public TokenValidationResponse requireRole(String authHeader, String... roles) {
        TokenValidationResponse auth = requireAuth(authHeader);
        if (auth.getRole() == null || Arrays.stream(roles).noneMatch(role -> role.equals(auth.getRole()))) {
            throw new ForbiddenException("Insufficient privileges");
        }
        return auth;
    }

    /**
     * Validates the token (alias for requireAuth with the response returned).
     */
    public TokenValidationResponse requireAuthenticated(String authHeader) {
        return requireAuth(authHeader);
    }

    private String stripBearer(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : header;
    }
}
