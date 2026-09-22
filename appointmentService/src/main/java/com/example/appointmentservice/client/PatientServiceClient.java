package com.example.appointmentservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.appointmentservice.dto.PatientDto;
import com.example.appointmentservice.exception.DownstreamDependencyException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * V-03 fix (CWE-209): Client for communicating with the Patient Service.
 *
 * Exception handling rules:
 * - Genuine HTTP 404 from Patient Service → returns null (patient does not exist).
 * - RestClientException (5xx, connection refused, timeout) → converted to
 *   DownstreamDependencyException with a static safe message and the original
 *   cause preserved for server-side logging. Never concatenates ex.getMessage(),
 *   response bodies, URLs, hostnames, or ports into any client-facing string.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.patient.base-url}")
    private String patientServiceUrl;

    public PatientDto getPatientById(UUID id) {
        String url = patientServiceUrl + "/api/patients/" + id;
        log.debug("Calling Patient service for patient id {}", id);
        try {
            return restTemplate.getForObject(url, PatientDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            // Patient Service confirmed the patient does not exist → caller handles as 404.
            log.debug("Patient not found for id {}", id);
            return null;
        } catch (RestClientException ex) {
            // Connection failure, timeout, or downstream 5xx.
            // Log with the full cause for operator visibility; never expose to client.
            log.error("Patient service call failed for patient {}", id, ex);
            throw new DownstreamDependencyException("Patient service unavailable", ex);
        }
    }
}
