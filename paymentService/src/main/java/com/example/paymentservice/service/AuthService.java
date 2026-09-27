package com.example.paymentservice.service;

import com.example.paymentservice.client.PatientServiceClient;
import com.example.paymentservice.client.UserServiceClient;
import com.example.paymentservice.dto.TokenValidationResponse;
import com.example.paymentservice.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    private final UserServiceClient userServiceClient;
    private final PatientServiceClient patientServiceClient;

    public AuthService(UserServiceClient userServiceClient, PatientServiceClient patientServiceClient) {
        this.userServiceClient = userServiceClient;
        this.patientServiceClient = patientServiceClient;
    }

    public TokenValidationResponse requireAuthenticated(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authentication required");
        }
        TokenValidationResponse auth;
        try {
            auth = userServiceClient.validate(authHeader);
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("Invalid token");
        }
        if (auth == null || !auth.isValid()) {
            throw new UnauthorizedException("Invalid token");
        }
        return auth;
    }

    public void requirePatientAccess(UUID patientId, String authHeader) {
        requireAuthenticated(authHeader);
        try {
            patientServiceClient.requirePatientAccess(patientId, authHeader);
        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden ex) {
            throw new com.example.paymentservice.exception.ForbiddenException("You cannot access this patient's payments");
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized ex) {
            throw new UnauthorizedException("Invalid token");
        }
    }
}
