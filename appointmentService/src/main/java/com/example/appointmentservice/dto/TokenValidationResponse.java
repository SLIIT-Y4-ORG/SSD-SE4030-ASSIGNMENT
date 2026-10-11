package com.example.appointmentservice.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class TokenValidationResponse {
    private boolean valid;
    private UUID userId;
    private String email;
    private String role;
    private String message;
}
