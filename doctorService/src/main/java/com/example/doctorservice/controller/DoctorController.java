package com.example.doctorservice.controller;

import com.example.doctorservice.model.Doctor;
import com.example.doctorservice.model.DoctorSlot;
import com.example.doctorservice.service.DoctorService;
import com.example.doctorservice.util.AuthHelper;
import com.example.doctorservice.client.UserServiceClient;
import com.example.doctorservice.dto.DoctorApplicationRequest;
import com.example.doctorservice.dto.TokenValidationResponse;
import jakarta.validation.Valid;
import com.example.doctorservice.exception.ForbiddenException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final AuthHelper authHelper;
    private final UserServiceClient userServiceClient;

    public DoctorController(DoctorService doctorService, AuthHelper authHelper, UserServiceClient userServiceClient) {
        this.doctorService = doctorService;
        this.authHelper = authHelper;
        this.userServiceClient = userServiceClient;
    }

    // post
    @PostMapping
    public ResponseEntity<Doctor> createDoctor(@RequestHeader("Authorization") String authHeader, @RequestBody Doctor doctor) {
        authHelper.requireRole(authHeader, "ADMIN", "RECEPTIONIST");
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createDoctor(doctor));
    }

    @PostMapping("/applications")
    public ResponseEntity<Doctor> submitApplication(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody DoctorApplicationRequest request) {
        TokenValidationResponse applicant = authHelper.requireRole(authHeader, "PATIENT");
        Doctor application = Doctor.builder()
                .userId(applicant.getUserId())
                .name(request.name().trim())
                .email(applicant.getEmail())
                .phone(request.phone() == null ? null : request.phone().trim())
                .specialization(request.specialization().trim())
                .licenseNumber(request.licenseNumber().trim())
                .verified(false)
                .isActive(false)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createApplication(application));
    }

    @GetMapping("/applications/me")
    public ResponseEntity<Doctor> getMyApplication(@RequestHeader("Authorization") String authHeader) {
        TokenValidationResponse caller = authHelper.requireAuthenticated(authHeader);
        return doctorService.getApplicationByUserId(caller.getUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/applications")
    public ResponseEntity<List<Doctor>> getPendingApplications(@RequestHeader("Authorization") String authHeader) {
        authHelper.requireRole(authHeader, "ADMIN", "RECEPTIONIST");
        return ResponseEntity.ok(doctorService.getPendingApplications());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Doctor> approveApplication(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        authHelper.requireRole(authHeader, "ADMIN");
        return ResponseEntity.ok(promoteAndApprove(id, authHelper.stripBearer(authHeader)));
    }

    // put
    @PutMapping("/{id}")
    public ResponseEntity<Doctor> updateDoctor(@RequestHeader("Authorization") String authHeader, @PathVariable UUID id, @RequestBody Doctor doctor) {
        authHelper.requireRole(authHeader, "ADMIN", "RECEPTIONIST");
        return ResponseEntity.ok(doctorService.updateDoctor(id, doctor));
    }

    // patch
    @PatchMapping("/{id}/verify")
    public ResponseEntity<Doctor> verifyDoctor(@RequestHeader("Authorization") String authHeader, @PathVariable UUID id, @RequestParam boolean verified) {
        authHelper.requireRole(authHeader, "ADMIN");
        if (verified) {
            return ResponseEntity.ok(promoteAndApprove(id, authHelper.stripBearer(authHeader)));
        }
        Doctor doctor = doctorService.getDoctorById(id);
        if (doctor.getUserId() != null) {
            userServiceClient.updateUserRole(doctor.getUserId(), "PATIENT", authHelper.stripBearer(authHeader));
        }
        return ResponseEntity.ok(doctorService.verifyDoctor(id, false));
    }

    // get
    @GetMapping
    public ResponseEntity<List<Doctor>> getAllDoctors(@RequestHeader("Authorization") String authHeader, @RequestParam(required = false) String specialization, @RequestParam(required = false) String department) {
        authHelper.requireAuthenticated(authHeader);
        return ResponseEntity.ok(doctorService.getAllDoctors(specialization, department));
    }

    // getDoctorById
    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getDoctorById(@RequestHeader("Authorization") String authHeader, @PathVariable UUID id) {
        authHelper.requireAuthenticated(authHeader);
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    // post
    @PostMapping("/{id}/slots")
    public ResponseEntity<List<DoctorSlot>> createSlots(@RequestHeader("Authorization") String authHeader, @PathVariable UUID id, @RequestBody List<DoctorSlot> slots) {
        TokenValidationResponse caller = authHelper.requireRole(authHeader, "ADMIN", "DOCTOR", "RECEPTIONIST");
        if ("DOCTOR".equals(caller.getRole())) {
            Doctor doctor = doctorService.getDoctorById(id);
            if (!doctor.isVerified() || !caller.getUserId().equals(doctor.getUserId())) {
                throw new ForbiddenException("Doctors may only manage their own verified schedule");
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createSlots(id, slots));
    }

    // getSlotById
    @GetMapping("/{id}/slots")
    public ResponseEntity<List<DoctorSlot>> getSlotsByDate(@RequestHeader("Authorization") String authHeader, @PathVariable UUID id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        authHelper.requireAuthenticated(authHeader);
        return ResponseEntity.ok(doctorService.getSlotsByDate(id, date));
    }

    // For Appointment Service Integration
    @GetMapping("/{id}/available-slots")
    public ResponseEntity<Doctor> getAvailableSlots(@PathVariable UUID id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(doctorService.getAvailableSlotsForAppointment(id, date));
    }

    // patch
    @PatchMapping("/{id}/link-user")
    public ResponseEntity<Doctor> linkUser(@RequestHeader("Authorization") String authHeader, @PathVariable UUID id, @RequestParam UUID userId) {
        authHelper.requireRole(authHeader, "ADMIN");
        return ResponseEntity.ok(doctorService.linkUser(id, userId));
    }

    private Doctor promoteAndApprove(UUID doctorId, String administratorToken) {
        Doctor pending = doctorService.getDoctorById(doctorId);
        if (pending.getUserId() == null) {
            throw new IllegalStateException("Doctor application is not linked to a user account");
        }
        userServiceClient.updateUserRole(pending.getUserId(), "DOCTOR", administratorToken);
        try {
            return doctorService.approveApplication(doctorId);
        } catch (RuntimeException approvalFailure) {
            try {
                userServiceClient.updateUserRole(pending.getUserId(), "PATIENT", administratorToken);
            } catch (RuntimeException ignored) {
                approvalFailure.addSuppressed(ignored);
            }
            throw approvalFailure;
        }
    }
}
