package com.example.appointmentservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.appointmentservice.exception.DownstreamDependencyException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * C-1 fix (CWE-306): Client for validating Bearer tokens against userService.
 *
 * Calls GET /api/auth/validate with the raw token in the Authorization header.
 * userService.AuthController.stripBearer() expects the full "Bearer <token>" value.
 *
 * Error-handling contract (mirrors PatientServiceClient):
 * - HTTP 401 from userService  → token is invalid; returns null.
 * - RestClientException (5xx, timeout, connection refused) → DownstreamDependencyException
 *   with a static safe message; original cause preserved for server-side logging only.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String userServiceUrl;

    /**
     * Validates a Bearer token.
     *
     * @param authHeader the full Authorization header value (e.g. "Bearer eyJ...")
     * @return a {@link TokenInfo} with userId and role if valid, or {@code null} if the
     *         token is invalid or expired.
     * @throws DownstreamDependencyException if the user service is unreachable or returns 5xx.
     */
    public TokenInfo validateToken(String authHeader) {
        String url = userServiceUrl + "/api/auth/validate";

        HttpHeaders headers = new HttpHeaders();
        // Forward the complete "Bearer <token>" value — userService strips the prefix itself.
        headers.set("Authorization", authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.debug("Calling userService token validation");
            ResponseEntity<TokenValidationResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, TokenValidationResponse.class);

            TokenValidationResponse body = response.getBody();
            if (body == null || !body.isValid()) {
                log.debug("Token validation returned invalid/null response");
                return null;
            }
            return new TokenInfo(body.getUserId(), body.getRole());

        } catch (HttpClientErrorException.Unauthorized ex) {
            // userService explicitly rejected the token — not a connectivity problem.
            log.debug("Token rejected by userService (401)");
            return null;
        } catch (RestClientException ex) {
            // Network failure, timeout, or unexpected 5xx — not safe to silently swallow.
            log.error("UserService token validation call failed", ex);
            throw new DownstreamDependencyException("Unable to validate credentials at this time", ex);
        }
    }

    // ─── Local response DTO ────────────────────────────────────────────────────

    /** Subset of userService TokenValidationResponse needed by this service. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenValidationResponse {
        private boolean valid;
        private UUID userId;
        private String role;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    /** Immutable token payload passed to controllers after successful validation. */
    public static final class TokenInfo {
        private final UUID userId;
        private final String role;

        public TokenInfo(UUID userId, String role) {
            this.userId = userId;
            this.role   = role;
        }

        public UUID getUserId() { return userId; }
        public String getRole()  { return role; }

        public boolean isAdmin()   { return "ADMIN".equalsIgnoreCase(role); }
        public boolean isPatient() { return "PATIENT".equalsIgnoreCase(role); }
        public boolean isDoctor()  { return "DOCTOR".equalsIgnoreCase(role); }
    }
}
