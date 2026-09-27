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
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.appointmentservice.dto.AppointmentResponse;
import com.example.appointmentservice.dto.CreateAppointmentRequest;
import com.example.appointmentservice.dto.RescheduleAppointmentRequest;
import com.example.appointmentservice.service.AppointmentService;
import com.example.appointmentservice.client.PatientServiceClient;
import com.example.appointmentservice.client.DoctorServiceClient;
import com.example.appointmentservice.dto.TokenValidationResponse;
import com.example.appointmentservice.exception.ForbiddenException;
import com.example.appointmentservice.util.AuthHelper;
import com.example.appointmentservice.util.InternalAuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthHelper authHelper;
    private final PatientServiceClient patientServiceClient;
    private final DoctorServiceClient doctorServiceClient;
    private final InternalAuthService internalAuthService;

    public AppointmentController(AppointmentService appointmentService, AuthHelper authHelper,
            PatientServiceClient patientServiceClient, DoctorServiceClient doctorServiceClient,
            InternalAuthService internalAuthService) {
        this.appointmentService = appointmentService;
        this.authHelper = authHelper;
        this.patientServiceClient = patientServiceClient;
        this.doctorServiceClient = doctorServiceClient;
        this.internalAuthService = internalAuthService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateAppointmentRequest request) {
        authHelper.requireRole(authHeader, "PATIENT");
        patientServiceClient.getPatientById(request.getPatientId(), authHeader);
        return ResponseEntity.ok(appointmentService.createAppointment(request, authHeader));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @RequestHeader("Authorization") String authHeader, @PathVariable UUID id) {
        AppointmentResponse appointment = appointmentService.getAppointmentById(id);
        authorizeAppointment(authHeader, appointment);
        return ResponseEntity.ok(appointment);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments(
            @RequestHeader("Authorization") String authHeader) {
        TokenValidationResponse auth = authHelper.requireAuthenticated(authHeader);
        return switch (auth.getRole()) {
            case "ADMIN", "RECEPTIONIST" -> ResponseEntity.ok(appointmentService.getAllAppointments());
            case "PATIENT" -> {
                var patient = patientServiceClient.getMyPatient(authHeader);
                yield ResponseEntity.ok(appointmentService.getAppointmentsForPatient(patient.getId()));
            }
            case "DOCTOR" -> {
                var doctor = doctorServiceClient.getMyDoctor(authHeader);
                yield ResponseEntity.ok(appointmentService.getAppointmentsForDoctor(doctor.getId()));
            }
            default -> throw new ForbiddenException("Insufficient privileges");
        };
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @RequestHeader("Authorization") String authHeader, @PathVariable UUID id) {
        authorizeAppointment(authHeader, appointmentService.getAppointmentById(id));
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
    public ResponseEntity<Map<String, Object>> initiatePaymentSession(
            @RequestHeader("Authorization") String authHeader, @PathVariable UUID id) {
        TokenValidationResponse auth = authHelper.requireRole(authHeader, "PATIENT");
        AppointmentResponse appointment = appointmentService.getAppointmentById(id);
        patientServiceClient.getPatientById(appointment.getPatientId(), authHeader);
        return ResponseEntity.ok(appointmentService.initiatePaymentSession(id));
    }

    @PostMapping("/payment-callback")
    public ResponseEntity<Map<String, String>> handlePaymentCallback(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @RequestBody Map<String, Object> payload) {
        internalAuthService.verify(internalApiKey);
        appointmentService.handlePaymentCallback(payload);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getAppointmentStatus(
            @RequestHeader("Authorization") String authHeader, @PathVariable UUID id) {
        authorizeAppointment(authHeader, appointmentService.getAppointmentById(id));
        return ResponseEntity.ok(appointmentService.getAppointmentStatus(id));
    }

    private void authorizeAppointment(String authHeader, AppointmentResponse appointment) {
        TokenValidationResponse auth = authHelper.requireAuthenticated(authHeader);
        if ("ADMIN".equals(auth.getRole()) || "RECEPTIONIST".equals(auth.getRole())) {
            return;
        }
        if ("PATIENT".equals(auth.getRole())) {
            patientServiceClient.getPatientById(appointment.getPatientId(), authHeader);
            return;
        }
        if ("DOCTOR".equals(auth.getRole())) {
            var doctor = doctorServiceClient.getDoctorById(appointment.getDoctorId(), authHeader);
            if (doctor != null && auth.getUserId().equals(doctor.getUserId())) {
                return;
            }
        }
        throw new ForbiddenException("You cannot access this appointment");
    }
}
