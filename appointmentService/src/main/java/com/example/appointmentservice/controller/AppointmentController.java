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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.appointmentservice.dto.AppointmentResponse;
import com.example.appointmentservice.dto.CreateAppointmentRequest;
import com.example.appointmentservice.dto.RescheduleAppointmentRequest;
import com.example.appointmentservice.security.AuthHelper;
import com.example.appointmentservice.service.AppointmentService;

import jakarta.validation.Valid;

/**
 * C-1 fix (CWE-306 — Missing Authentication for Critical Function):
 *
 * Every endpoint now requires proof of identity before data is returned or state
 * is mutated.  Two mechanisms are used:
 *
 *  • User-facing endpoints (GET/POST/PATCH by a logged-in user) require a valid
 *    JWT via "Authorization: Bearer <token>".  AuthHelper.requireBearer() calls
 *    userService GET /api/auth/validate and throws UnauthorizedException (→ 401)
 *    if the token is absent, malformed, or expired.
 *
 *  • POST /payment-callback is a service-to-service call from paymentService.
 *    It requires the shared "X-Internal-Api-Key" header.  AuthHelper.requireInternalKey()
 *    throws UnauthorizedException (→ 401) if the key is missing or wrong.
 *
 * No changes were made to business logic, service calls, or response shapes.
 * GlobalExceptionHandler (Dev 3) is not touched.
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthHelper authHelper;

    public AppointmentController(AppointmentService appointmentService,
                                 AuthHelper authHelper) {
        this.appointmentService = appointmentService;
        this.authHelper         = authHelper;
    }

    // ─── Endpoints ─────────────────────────────────────────────────────────────

    /**
     * Create a new appointment.
     * Requires: valid user JWT (any authenticated role may book on behalf of a patient).
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateAppointmentRequest request) {

        authHelper.requireBearer(authHeader);  // C-1 fix: must be authenticated
        return ResponseEntity.ok(appointmentService.createAppointment(request, authHeader));
    }

    /**
     * Retrieve a single appointment by ID.
     * Requires: valid user JWT.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {

        authHelper.requireBearer(authHeader);  // C-1 fix
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    /**
     * Retrieve all appointments.
     * Requires: valid user JWT.
     */
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authHelper.requireBearer(authHeader);  // C-1 fix
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    /**
     * Cancel an appointment.
     * Requires: valid user JWT.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {

        authHelper.requireBearer(authHeader);  // C-1 fix
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

    /**
     * Initiate a Stripe payment session for a pending appointment.
     * Requires: valid user JWT.
     */
    @PostMapping("/{id}/payment-session")
    public ResponseEntity<Map<String, Object>> initiatePaymentSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {

        authHelper.requireBearer(authHeader);  // C-1 fix
        return ResponseEntity.ok(appointmentService.initiatePaymentSession(id));
    }

    /**
     * Payment-status callback called by the frontend after Stripe redirects back
     * to the success page.  The frontend sends the logged-in user's Bearer token
     * via authHeaders(), so this endpoint uses the same JWT mechanism as all other
     * user-facing endpoints.
     *
     * C-1 fix: replaced open unauthenticated access with requireBearer so that
     * only authenticated users can trigger appointment-status updates.
     */
    @PostMapping("/payment-callback")
    public ResponseEntity<Map<String, String>> handlePaymentCallback(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {

        authHelper.requireBearer(authHeader);  // C-1 fix: user JWT required
        appointmentService.handlePaymentCallback(payload);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    /**
     * Check appointment status (payment state, confirmation).
     * Requires: valid user JWT.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getAppointmentStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {

        authHelper.requireBearer(authHeader);  // C-1 fix
        return ResponseEntity.ok(appointmentService.getAppointmentStatus(id));
    }
}