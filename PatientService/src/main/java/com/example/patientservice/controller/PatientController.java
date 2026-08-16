package com.example.patientservice.controller;

import com.example.patientservice.dto.TokenValidationResponse;
import com.example.patientservice.model.Patient;
import com.example.patientservice.service.PatientService;
import com.example.patientservice.util.AuthHelper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final AuthHelper authHelper;

    public PatientController(PatientService patientService, AuthHelper authHelper) {
        this.patientService = patientService;
        this.authHelper = authHelper;
    }

    /**
     * Create a new patient profile (linked to the authenticated user).
     * Any authenticated user can create their own patient profile.
     */
    @PostMapping
    public ResponseEntity<Patient> createPatient(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody Patient patient) {
        TokenValidationResponse auth = authHelper.requireAuthenticated(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.createPatient(patient, auth.getUserId()));
    }

    /**
     * Get all patients. Restricted to ADMIN and RECEPTIONIST roles.
     */
    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients(
            @RequestHeader("Authorization") String authHeader) {
        authHelper.requireRole(authHeader, "ADMIN", "RECEPTIONIST");
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    /**
     * Get the current user's patient profile.
     */
    @GetMapping("/me")
    public ResponseEntity<Patient> getMyProfile(
            @RequestHeader("Authorization") String authHeader) {
        TokenValidationResponse auth = authHelper.requireAuthenticated(authHeader);
        return ResponseEntity.ok(patientService.getPatientByUserId(auth.getUserId()));
    }

    /**
     * Get a patient by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        authHelper.requireAuthenticated(authHeader);
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    /**
     * Update a patient profile. Only the patient themselves, ADMIN, or RECEPTIONIST can update.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @RequestBody Patient patient) {
        TokenValidationResponse auth = authHelper.requireAuthenticated(authHeader);
        // Allow self-update or admin/receptionist
        Patient existing = patientService.getPatientById(id);
        if (!existing.getUserId().equals(auth.getUserId())
                && !"ADMIN".equals(auth.getRole())
                && !"RECEPTIONIST".equals(auth.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(patientService.updatePatient(id, patient));
    }
}
