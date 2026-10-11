package com.example.appointmentservice.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class PatientDto {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
