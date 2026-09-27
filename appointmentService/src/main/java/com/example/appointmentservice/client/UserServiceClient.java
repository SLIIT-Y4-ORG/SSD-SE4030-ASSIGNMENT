package com.example.appointmentservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.appointmentservice.dto.TokenValidationResponse;
import com.example.appointmentservice.exception.DownstreamDependencyException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String userServiceUrl;

    public TokenValidationResponse validateToken(String authHeader) {
        String url = userServiceUrl + "/api/auth/validate";

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
            throw new DownstreamDependencyException("Unable to validate credentials at this time", ex);
        }
    }

    public static class TokenInfo extends TokenValidationResponse {
        public TokenInfo(java.util.UUID userId, String role) {
            setValid(true);
            setUserId(userId);
            setRole(role);
        }
    }
}
