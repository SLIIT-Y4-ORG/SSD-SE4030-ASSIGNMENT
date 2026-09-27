package com.example.appointmentservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.appointmentservice.client.UserServiceClient;
import com.example.appointmentservice.dto.TokenValidationResponse;
import com.example.appointmentservice.exception.DownstreamDependencyException;
import com.example.appointmentservice.exception.ServiceUnavailableException;
import com.example.appointmentservice.exception.UnauthorizedException;

public class AuthHelper {

    private static final Logger log = LoggerFactory.getLogger(AuthHelper.class);

    private final UserServiceClient userServiceClient;

    public AuthHelper(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    public TokenValidationResponse requireBearer(String authHeader) {
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new UnauthorizedException("Authentication required");
        }
        try {
            TokenValidationResponse info = userServiceClient.validateToken(authHeader);
            if (info == null || !info.isValid()) {
                throw new UnauthorizedException("Authentication required");
            }
            return info;
        } catch (DownstreamDependencyException ex) {
            log.error("Cannot reach userService for token validation", ex);
            throw new ServiceUnavailableException("Unable to validate credentials at this time");
        }
    }
}
