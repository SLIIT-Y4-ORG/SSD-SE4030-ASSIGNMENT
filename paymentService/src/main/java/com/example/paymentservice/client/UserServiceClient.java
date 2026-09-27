package com.example.paymentservice.client;

import com.example.paymentservice.dto.TokenValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserServiceClient(RestTemplate restTemplate,
            @Value("${services.user.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public TokenValidationResponse validate(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        return restTemplate.exchange(baseUrl + "/api/auth/validate", HttpMethod.GET,
                new HttpEntity<>(headers), TokenValidationResponse.class).getBody();
    }
}
