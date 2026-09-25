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

import com.example.appointmentservice.dto.DoctorDto;
import com.example.appointmentservice.exception.DownstreamDependencyException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * V-03 fix (CWE-209): Client for communicating with the Doctor Service.
 *
 * Token propagation: the Authorization header from the incoming frontend request
 * is forwarded verbatim (including the Bearer prefix). The token is never logged,
 * stored, or included in exception messages.
 *
 * Exception handling rules (V-03 preserved):
 * - Genuine HTTP 404 from Doctor Service → returns null (doctor does not exist).
 * - HTTP 401/403 (auth failure) → DownstreamDependencyException.
 * - RestClientException (5xx, connection refused, timeout) → DownstreamDependencyException
 *   with a static safe message and the original cause preserved for server-side logging.
 * - Raw downstream messages, URLs, hostnames, or ports are never concatenated into
 *   any client-facing string.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.doctor.base-url}")
    private String doctorServiceUrl;

    /**
     * Retrieves a doctor by ID, forwarding the caller's Authorization header.
     *
     * @param id                  the doctor UUID
     * @param authorizationHeader the complete "Authorization: Bearer ..." header
     *                            received from the incoming frontend request
     * @return the DoctorDto, or {@code null} if the doctor genuinely does not exist
     * @throws DownstreamDependencyException if the Doctor Service is unreachable or
     *         returns an unexpected error (including auth failures)
     */
    public DoctorDto getDoctorById(UUID id, String authorizationHeader) {
        String url = doctorServiceUrl + "/api/doctors/" + id;
        log.debug("Calling Doctor service for doctor id {}", id);

        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<DoctorDto> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, DoctorDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            // Doctor Service confirmed the doctor does not exist → caller handles as 404.
            log.debug("Doctor not found for id {}", id);
            return null;
        } catch (RestClientException ex) {
            // Connection failure, timeout, downstream 5xx, or 401/403.
            // Log the cause for operator visibility — never include the token in the log message.
            log.error("Doctor service call failed for doctor {}", id, ex);
            throw new DownstreamDependencyException("Doctor service unavailable", ex);
        }
    }
}
