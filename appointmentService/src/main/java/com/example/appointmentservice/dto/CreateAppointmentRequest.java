package com.example.appointmentservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * CWE-20 fix: 'amount' and 'currency' fields have been removed from this DTO.
 *
 * Previously, the client supplied the payment amount directly in the request
 * body. This allowed any authenticated patient to manipulate the price
 * (e.g. amount=0.01) and trigger a Stripe checkout session for a fraction
 * of the actual consultation fee.
 *
 * The server now assigns a fixed, authoritative consultation fee internally
 * in AppointmentServiceImpl. No client-supplied amount is accepted or stored.
 *
 * Any 'amount' or 'currency' fields sent by older clients are silently ignored
 * by Jackson's default unknown-property behaviour (FAIL_ON_UNKNOWN_PROPERTIES=false).
 */
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
}