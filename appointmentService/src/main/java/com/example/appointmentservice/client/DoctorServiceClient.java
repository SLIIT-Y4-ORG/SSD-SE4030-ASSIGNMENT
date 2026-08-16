package com.example.appointmentservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

    public DoctorDto getDoctorById(UUID id) {
        try {
            String url = doctorServiceUrl + "/api/doctors/" + id;
            log.debug("Calling Doctor service: {}", url);
            return restTemplate.getForObject(url, DoctorDto.class);
        } catch (Exception e) {
            log.error("Error calling Doctor service for id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to fetch doctor: " + e.getMessage(), e);
        }
    }
}
