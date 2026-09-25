package com.example.appointmentservice.security;

import com.example.appointmentservice.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

/**
 * Validates the incoming Authorization header for Appointment Service endpoints
 * that forward the token to downstream services.
 *
 * Rules:
 * - Header must be non-null and non-blank.
 * - Header must start with "Bearer " (case-sensitive, one space).
 * - The token after "Bearer " must be non-blank.
 *
 * A {@link UnauthorizedException} is thrown for any violation; the
 * {@link com.example.appointmentservice.exception.GlobalExceptionHandler}
 * converts it to HTTP 401 with the static message "Authentication required".
 *
 * The validated header value is never logged.
 */
@Component
public class AuthorizationHeaderValidator {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Validates that {@code authorizationHeader} is present, non-blank, and
     * starts with the "Bearer " prefix, with a non-blank token following it.
     *
     * @param authorizationHeader the raw value of the Authorization header,
     *                            may be null when {@code required=false} is used
     * @throws UnauthorizedException if the header is absent, blank, or malformed
     */
    public void validate(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Authentication required");
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        if (token.isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
    }
}
