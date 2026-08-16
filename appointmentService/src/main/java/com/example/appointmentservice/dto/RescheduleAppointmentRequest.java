package com.example.appointmentservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RescheduleAppointmentRequest {

    @NotNull
    private UUID doctorId;

    @NotNull
    private UUID newSlotId;
}