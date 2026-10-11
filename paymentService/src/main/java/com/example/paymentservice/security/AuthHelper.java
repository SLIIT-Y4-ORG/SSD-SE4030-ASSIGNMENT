package com.example.paymentservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.paymentservice.client.UserServiceClient;
import com.example.paymentservice.client.UserServiceClient.TokenInfo;
import com.example.paymentservice.exception.UnauthorizedException;

/**
 * C-7 fix (CWE-306): Centralised authentication helper for PaymentController.
 *
 * {@link #requireBearer(String)} validates the "Authorization: Bearer <token>" header
 * by delegating to {@link UserServiceClient} (which calls userService GET /api/auth/validate).
 *
 * On failure throws {@link UnauthorizedException}, which GlobalExceptionHandler
 * maps to HTTP 401 Unauthorized with a static safe message.
 */
@Component
public class AuthHelper {

    private static final Logger log = LoggerFactory.getLogger(AuthHelper.class);

    private final UserServiceClient userServiceClient;

    public AuthHelper(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    /**
     * Validates the {@code Authorization: Bearer <token>} header.
     *
     * @param authHeader the full header value (may be null)
     * @return the resolved {@link TokenInfo} for the authenticated caller
     * @throws UnauthorizedException if the header is missing, malformed, or the token is invalid/expired
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
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Cannot reach userService for token validation", ex);
            // Re-throw as UnauthorizedException so we don't leak internal error detail.
            // GlobalExceptionHandler will catch the generic Exception and return 500
            // if this is re-thrown as RuntimeException — so wrap it for a cleaner 401.
            throw new UnauthorizedException("Authentication required");
        }
    }
}
