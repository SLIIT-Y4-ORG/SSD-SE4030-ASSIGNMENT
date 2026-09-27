package com.example.appointmentservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.appointmentservice.client.UserServiceClient;
import com.example.appointmentservice.client.UserServiceClient.TokenInfo;
import com.example.appointmentservice.exception.ServiceUnavailableException;
import com.example.appointmentservice.exception.UnauthorizedException;
import com.example.appointmentservice.exception.DownstreamDependencyException;

import lombok.RequiredArgsConstructor;

/**
 * C-1 fix (CWE-306): Centralised authentication helper for AppointmentController.
 *
 * One entry point:
 *
 *  {@link #requireBearer(String)} — enforces a valid user JWT on all endpoints.
 *
 * Throws {@link UnauthorizedException} on failure, which
 * GlobalExceptionHandler maps to HTTP 401 Unauthorized.
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private static final Logger log = LoggerFactory.getLogger(AuthHelper.class);

    private final UserServiceClient userServiceClient;

    /**
     * Validates the {@code Authorization: Bearer <token>} header.
     *
     * @param authHeader the full header value (may be null)
     * @return the resolved {@link TokenInfo} for the caller
     * @throws UnauthorizedException     if the header is missing, malformed, or the token is invalid
     * @throws ServiceUnavailableException if userService is unreachable (propagated from client)
     */
    public TokenInfo requireBearer(String authHeader) {
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new UnauthorizedException("Authentication required");
        }
        try {
            TokenInfo info = userServiceClient.validateToken(authHeader);
            if (info == null) {
                throw new UnauthorizedException("Authentication required");
            }
            return info;
        } catch (DownstreamDependencyException ex) {
            log.error("Cannot reach userService for token validation", ex);
            throw new ServiceUnavailableException("Unable to validate credentials at this time");
        }
    }
}
