package com.example.appointmentservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
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
 * Exception handling rules:
 * - Genuine HTTP 404 from Doctor Service → returns null (doctor does not exist).
 * - RestClientException (5xx, connection refused, timeout) → converted to
 *   DownstreamDependencyException with a static safe message and the original
 *   cause preserved for server-side logging. Never concatenates ex.getMessage(),
 *   response bodies, URLs, hostnames, or ports into any client-facing string.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.doctor.base-url}")
    private String doctorServiceUrl;

    public DoctorDto getDoctorById(UUID id) {
        String url = doctorServiceUrl + "/api/doctors/" + id;
        log.debug("Calling Doctor service for doctor id {}", id);
        try {
            return restTemplate.getForObject(url, DoctorDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            // Doctor Service confirmed the doctor does not exist → caller handles as 404.
            log.debug("Doctor not found for id {}", id);
            return null;
        } catch (RestClientException ex) {
            // Connection failure, timeout, or downstream 5xx.
            // Log with the full cause for operator visibility; never expose to client.
            log.error("Doctor service call failed for doctor {}", id, ex);
            throw new DownstreamDependencyException("Doctor service unavailable", ex);
        }
    }
}
