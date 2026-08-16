package com.example.doctorservice.client;

import com.example.doctorservice.dto.TokenValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

    private TokenValidationResponse invalid(String message) {
        return TokenValidationResponse.builder().valid(false).message(message).build();
    }
}
