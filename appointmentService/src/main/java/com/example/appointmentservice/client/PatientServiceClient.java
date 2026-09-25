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

/**
 * V-03 fix (CWE-209): Client for communicating with the Patient Service.
 *
 * Token propagation: the Authorization header from the incoming frontend request
 * is forwarded verbatim (including the Bearer prefix). The token is never logged,
 * stored, or included in exception messages.
 *
 * Exception handling rules (V-03 preserved):
 * - Genuine HTTP 404 from Patient Service → returns null (patient does not exist).
 * - HTTP 401/403 (auth failure) → DownstreamDependencyException (same as 5xx;
 *   the caller reports "Unable to validate patient at this time").
 * - RestClientException (5xx, connection refused, timeout) → DownstreamDependencyException
 *   with a static safe message and the original cause preserved for server-side logging.
 * - Raw downstream messages, URLs, hostnames, or ports are never concatenated into
 *   any client-facing string.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.patient.base-url}")
    private String patientServiceUrl;

    /**
     * Retrieves a patient by ID, forwarding the caller's Authorization header.
     *
     * @param id                  the patient UUID
     * @param authorizationHeader the complete "Authorization: Bearer ..." header
     *                            received from the incoming frontend request
     * @return the PatientDto, or {@code null} if the patient genuinely does not exist
     * @throws DownstreamDependencyException if the Patient Service is unreachable or
     *         returns an unexpected error (including auth failures)
     */
    public PatientDto getPatientById(UUID id, String authorizationHeader) {
        String url = patientServiceUrl + "/api/patients/" + id;
        log.debug("Calling Patient service for patient id {}", id);

        HttpHeaders headers = new HttpHeaders();
        // Forward the complete header value including the Bearer prefix.
        // Never log the token value.
        if (authorizationHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<PatientDto> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, PatientDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            // Patient Service confirmed the patient does not exist → caller handles as 404.
            log.debug("Patient not found for id {}", id);
            return null;
        } catch (RestClientException ex) {
            // Connection failure, timeout, downstream 5xx, or 401/403.
            // Log the cause for operator visibility — never include the token in the log message.
            log.error("Patient service call failed for patient {}", id, ex);
            throw new DownstreamDependencyException("Patient service unavailable", ex);
        }
    }
}
