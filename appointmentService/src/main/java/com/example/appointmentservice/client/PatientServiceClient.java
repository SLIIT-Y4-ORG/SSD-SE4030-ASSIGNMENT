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

import com.example.appointmentservice.dto.PatientDto;
import com.example.appointmentservice.exception.DownstreamDependencyException;

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
        String url = patientServiceUrl + "/api/patients/" + id;
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }

        try {
            ResponseEntity<PatientDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), PatientDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            log.debug("Patient not found for id {}", id);
            return null;
        } catch (RestClientException ex) {
            log.error("Patient service call failed for patient {}", id, ex);
            throw new DownstreamDependencyException("Patient service unavailable", ex);
        }
    }

    public PatientDto getMyPatient(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        return restTemplate.exchange(patientServiceUrl + "/api/patients/me", HttpMethod.GET,
                new HttpEntity<>(headers), PatientDto.class).getBody();
    }
}
