package com.example.doctorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoctorApplicationRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 100) String specialization,
        @NotBlank @Size(max = 100) String licenseNumber) {
}
