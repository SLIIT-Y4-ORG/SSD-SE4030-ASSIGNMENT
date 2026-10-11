package com.example.paymentservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class PatientServiceClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PatientServiceClient(RestTemplate restTemplate,
            @Value("${services.patient.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void requirePatientAccess(UUID patientId, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        restTemplate.exchange(baseUrl + "/api/patients/" + patientId, HttpMethod.GET,
                new HttpEntity<>(headers), Object.class);
    }
}
