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

@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.doctor.base-url}")
    private String doctorServiceUrl;

    public DoctorDto getDoctorById(UUID id, String authHeader) {
        String url = doctorServiceUrl + "/api/doctors/" + id;
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }

        try {
            ResponseEntity<DoctorDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), DoctorDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            log.debug("Doctor not found for id {}", id);
            return null;
        } catch (RestClientException ex) {
            log.error("Doctor service call failed for doctor {}", id, ex);
            throw new DownstreamDependencyException("Doctor service unavailable", ex);
        }
    }

    public DoctorDto getMyDoctor(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        return restTemplate.exchange(doctorServiceUrl + "/api/doctors/applications/me", HttpMethod.GET,
                new HttpEntity<>(headers), DoctorDto.class).getBody();
    }
}
