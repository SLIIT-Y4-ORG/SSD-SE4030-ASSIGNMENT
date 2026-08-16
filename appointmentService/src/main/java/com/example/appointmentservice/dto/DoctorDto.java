package com.example.appointmentservice.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class DoctorDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String specialty;
    private String email;
}
