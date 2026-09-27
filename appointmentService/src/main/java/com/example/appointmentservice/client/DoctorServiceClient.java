package com.example.appointmentservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import com.example.appointmentservice.dto.DoctorDto;

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
        try {
            String url = doctorServiceUrl + "/api/doctors/" + id;
            log.debug("Calling Doctor service: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), DoctorDto.class).getBody();
        } catch (Exception e) {
            log.error("Error calling Doctor service for id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to fetch doctor: " + e.getMessage(), e);
        }
    }

    public DoctorDto getMyDoctor(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        return restTemplate.exchange(doctorServiceUrl + "/api/doctors/applications/me", HttpMethod.GET,
                new HttpEntity<>(headers), DoctorDto.class).getBody();
    }
}
