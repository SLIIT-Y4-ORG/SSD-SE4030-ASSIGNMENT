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
     * Patients can create their own profile. Identity is always taken from the token.
     */
    @PostMapping
    public ResponseEntity<Patient> createPatient(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody Patient patient) {
        TokenValidationResponse auth = authHelper.requireRole(authHeader, "PATIENT");
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
     * Get a patient by ID. Restricted to the owner or authorized staff.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        TokenValidationResponse auth = authHelper.requireAuthenticated(authHeader);
        Patient patient = patientService.getPatientById(id);
        requireOwnerOrStaff(auth, patient);
        return ResponseEntity.ok(patient);
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
        requireOwnerOrStaff(auth, existing);
        return ResponseEntity.ok(patientService.updatePatient(id, patient));
    }

    private void requireOwnerOrStaff(TokenValidationResponse auth, Patient patient) {
        boolean owner = patient.getUserId() != null && patient.getUserId().equals(auth.getUserId());
        boolean staff = "ADMIN".equals(auth.getRole()) || "RECEPTIONIST".equals(auth.getRole());
        if (!owner && !staff) {
            throw new com.example.patientservice.exception.ForbiddenException(
                    "You may only access your own patient profile");
        }
    }
}
