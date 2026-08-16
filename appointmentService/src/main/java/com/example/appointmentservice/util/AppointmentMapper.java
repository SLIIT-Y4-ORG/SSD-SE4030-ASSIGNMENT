package com.example.appointmentservice.util;

import com.example.appointmentservice.dto.AppointmentResponse;
import com.example.appointmentservice.model.Appointment;

public class AppointmentMapper {

    private AppointmentMapper() {}

    public static AppointmentResponse toResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatientId())
                .doctorId(appointment.getDoctorId())
                .slotId(appointment.getSlotId())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus().name())
                .paymentStatus(appointment.getPaymentStatus() != null ? appointment.getPaymentStatus().name() : null)
                .paymentTransactionId(appointment.getPaymentTransactionId())
                .stripeSessionId(appointment.getStripeSessionId())
                .amount(appointment.getAmount())
                .currency(appointment.getCurrency())
                .reason(appointment.getReason())
                .notes(appointment.getNotes())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}