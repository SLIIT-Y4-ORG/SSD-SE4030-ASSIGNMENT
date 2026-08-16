package com.example.appointmentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
public class CreateAppointmentRequest {

    @NotNull(message = "patientId is required")
    private UUID patientId;

    @NotNull(message = "doctorId is required")
    private UUID doctorId;

    @NotNull(message = "slotId is required")
    private UUID slotId;

    @NotBlank(message = "reason is required")
    private String reason;

    private String notes;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    private String currency;
}