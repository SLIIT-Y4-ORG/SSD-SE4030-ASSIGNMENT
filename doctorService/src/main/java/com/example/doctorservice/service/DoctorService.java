package com.example.doctorservice.service;

import com.example.doctorservice.model.Doctor;
import com.example.doctorservice.model.DoctorSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DoctorService {

    Doctor createDoctor(Doctor doctor);

    Doctor getDoctorById(UUID id);

    List<Doctor> getAllDoctors(String specialization, String department);

    List<DoctorSlot> createSlots(UUID doctorId, List<DoctorSlot> slots);

    List<DoctorSlot> getSlotsByDate(UUID doctorId, LocalDate date);

    Doctor linkUser(UUID doctorId, UUID userId);

    Doctor updateDoctor(UUID id, Doctor doctor);

    Doctor verifyDoctor(UUID id, boolean verified);

    Doctor getAvailableSlotsForAppointment(UUID doctorId, LocalDate date);
}
