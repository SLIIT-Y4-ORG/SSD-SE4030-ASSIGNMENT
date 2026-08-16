package com.example.appointmentservice.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.appointmentservice.dto.AppointmentResponse;
import com.example.appointmentservice.dto.CreateAppointmentRequest;

public interface AppointmentService {

    AppointmentResponse createAppointment(CreateAppointmentRequest request);

    AppointmentResponse getAppointmentById(UUID id);

    List<AppointmentResponse> getAllAppointments();

    AppointmentResponse cancelAppointment(UUID id);

    Map<String, Object> initiatePaymentSession(UUID appointmentId);

    void handlePaymentCallback(Map<String, Object> payload);

    Map<String, Object> getAppointmentStatus(UUID appointmentId);
}