package com.example.doctorservice.client;

import com.example.doctorservice.dto.TokenValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserServiceClient {
    private final RestTemplate restTemplate;
    @Value("${user-service.url}")
    private String userServiceUrl;

    public TokenValidationResponse validateToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return invalid("No token provided");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<TokenValidationResponse> response = restTemplate.exchange(
                    userServiceUrl + "/api/auth/validate", HttpMethod.GET, entity, TokenValidationResponse.class);
            TokenValidationResponse body = response.getBody();
            if (body == null) {
                return invalid("Empty response");
            }
            return body;
        } catch (Exception e) {
            return invalid("Validation failed");
        }
    }

    public void updateUserRole(UUID userId, String role, String administratorToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(administratorToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("role", role), headers);
        restTemplate.exchange(
                userServiceUrl + "/api/users/" + userId + "/role",
                HttpMethod.PATCH,
                entity,
                Void.class);
    }

    private TokenValidationResponse invalid(String message) {
        return TokenValidationResponse.builder().valid(false).message(message).build();
    }
}
