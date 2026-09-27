package com.example.appointmentservice.client;

import com.example.appointmentservice.dto.TokenValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceClient {
    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String userServiceUrl;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TokenValidationResponse validateToken(String bearerHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerHeader);
        return restTemplate.exchange(userServiceUrl + "/api/auth/validate", HttpMethod.GET,
                new HttpEntity<>(headers), TokenValidationResponse.class).getBody();
    }
}
