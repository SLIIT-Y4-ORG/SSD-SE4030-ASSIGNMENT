package com.example.appointmentservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.appointmentservice.dto.AppointmentResponse;
import com.example.appointmentservice.dto.CreateAppointmentRequest;
import com.example.appointmentservice.dto.RescheduleAppointmentRequest;
import com.example.appointmentservice.service.AppointmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.createAppointment(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }

    // @PatchMapping("/{id}/reschedule")
    // public ResponseEntity<AppointmentResponse> rescheduleAppointment(
    //         @PathVariable UUID id,
    //         @Valid @RequestBody RescheduleAppointmentRequest request) {
    //     return ResponseEntity.ok(appointmentService.rescheduleAppointment(id, request));
    // }

    // @GetMapping("/stats")
    // public ResponseEntity<HashMap<String, Object>> getAppointmentStats() {
    //     return ResponseEntity.ok(appointmentService.getAppointmentStats());
    // }

    @PostMapping("/{id}/payment-session")
    public ResponseEntity<Map<String, Object>> initiatePaymentSession(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.initiatePaymentSession(id));
    }

    @PostMapping("/payment-callback")
    public ResponseEntity<Map<String, String>> handlePaymentCallback(@RequestBody Map<String, Object> payload) {
        appointmentService.handlePaymentCallback(payload);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getAppointmentStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentStatus(id));
    }
}