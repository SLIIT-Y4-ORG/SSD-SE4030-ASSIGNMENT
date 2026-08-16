package com.example.appointmentservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppointmentResponse {

    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private UUID slotId;

    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private String status;
    private String paymentStatus;
    private UUID paymentTransactionId;
    private String stripeSessionId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private String notes;

    private Instant createdAt;
    private Instant updatedAt;
}