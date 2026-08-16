package com.example.doctorservice.controller;

import com.example.doctorservice.model.Doctor;
import com.example.doctorservice.model.DoctorSlot;
import com.example.doctorservice.service.DoctorService;
import com.example.doctorservice.util.AuthHelper;
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

    public DoctorController(DoctorService doctorService, AuthHelper authHelper) {
        this.doctorService = doctorService;
        this.authHelper = authHelper;
    }

    // post
    @PostMapping
    public ResponseEntity<Doctor> createDoctor(@RequestHeader("Authorization") String authHeader, @RequestBody Doctor doctor) {
        authHelper.requireRole(authHeader, "ADMIN", "RECEPTIONIST", "DOCTOR");
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createDoctor(doctor));
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
        authHelper.requireRole(authHeader, "ADMIN", "RECEPTIONIST");
        return ResponseEntity.ok(doctorService.verifyDoctor(id, verified));
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
        authHelper.requireRole(authHeader, "ADMIN", "DOCTOR", "RECEPTIONIST");
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
        authHelper.requireAuth(authHeader);
        return ResponseEntity.ok(doctorService.linkUser(id, userId));
    }
}