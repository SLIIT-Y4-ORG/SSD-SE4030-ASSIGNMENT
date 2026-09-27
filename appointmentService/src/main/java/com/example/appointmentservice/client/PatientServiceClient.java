package com.example.appointmentservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import com.example.appointmentservice.dto.PatientDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.patient.base-url}")
    private String patientServiceUrl;

    public PatientDto getPatientById(UUID id, String authHeader) {
        try {
            String url = patientServiceUrl + "/api/patients/" + id;
            log.debug("Calling Patient service: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), PatientDto.class).getBody();
        } catch (Exception e) {
            log.error("Error calling Patient service for id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to fetch patient: " + e.getMessage(), e);
        }
    }

    public PatientDto getMyPatient(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        return restTemplate.exchange(patientServiceUrl + "/api/patients/me", HttpMethod.GET,
                new HttpEntity<>(headers), PatientDto.class).getBody();
    }
}
