package com.example.paymentservice.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * C-7 fix (CWE-306): HTTP client for validating Bearer tokens against userService.
 *
 * Calls GET /api/auth/validate with the full "Authorization: Bearer <token>" header.
 * userService.AuthController.stripBearer() expects the full prefixed value.
 *
 * Error-handling contract:
 * - HTTP 401 from userService  → token invalid/expired; returns null (not a network error).
 * - RestClientException (5xx, timeout, refused) → logs and re-throws as RuntimeException
 *   with a static message; original cause is preserved for server-side logging only.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String userServiceUrl;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Validates a Bearer token by calling userService.
     *
     * @param authHeader the full Authorization header value (e.g. "Bearer eyJ...")
     * @return a {@link TokenInfo} with userId and role if valid, or {@code null} if invalid/expired.
     * @throws RuntimeException with a static message if userService is unreachable.
     */
    public TokenInfo validateToken(String authHeader) {
        String url = userServiceUrl + "/api/auth/validate";

        HttpHeaders headers = new HttpHeaders();
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
            log.debug("Token rejected by userService (401)");
            return null;
        } catch (RestClientException ex) {
            log.error("UserService token validation call failed", ex);
            throw new RuntimeException("Unable to validate credentials at this time", ex);
        }
    }

    // ─── Local response DTO ────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenValidationResponse {
        private boolean valid;
        private UUID userId;
        private String role;

        public boolean isValid()            { return valid; }
        public void setValid(boolean v)     { this.valid = v; }
        public UUID getUserId()             { return userId; }
        public void setUserId(UUID id)      { this.userId = id; }
        public String getRole()             { return role; }
        public void setRole(String r)       { this.role = r; }
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
    }
}
