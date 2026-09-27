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

import com.example.paymentservice.dto.TokenValidationResponse;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserServiceClient(RestTemplate restTemplate,
            @Value("${services.user.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public TokenValidationResponse validate(String authHeader) {
        String url = baseUrl + "/api/auth/validate";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);

        try {
            ResponseEntity<TokenValidationResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), TokenValidationResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.debug("Token rejected by userService");
            return null;
        } catch (RestClientException ex) {
            log.error("UserService token validation call failed", ex);
            throw new RuntimeException("Unable to validate credentials at this time", ex);
        }
    }

    public TokenInfo validateToken(String authHeader) {
        TokenValidationResponse response = validate(authHeader);
        if (response == null || !response.isValid()) {
            return null;
        }
        return new TokenInfo(response.getUserId(), response.getRole());
    }

    public static final class TokenInfo {
        private final UUID userId;
        private final String role;

        public TokenInfo(UUID userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        public UUID getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }
    }
}
