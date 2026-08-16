package com.example.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {

    private boolean valid;
    private UUID userId;
    private String email;

    /** One of: ADMIN, DOCTOR, PATIENT, RECEPTIONIST — as defined in user-service. */
    private String role;

    private String message;
}
