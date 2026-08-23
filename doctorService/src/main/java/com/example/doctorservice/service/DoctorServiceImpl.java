package com.example.doctorservice.service;

import com.example.doctorservice.exception.DoctorNotFoundException;
import com.example.doctorservice.exception.DuplicateDoctorEmailException;
import com.example.doctorservice.model.Doctor;
import com.example.doctorservice.model.DoctorSlot;
import com.example.doctorservice.model.SlotStatus;
import com.example.doctorservice.repository.DoctorRepository;
import com.example.doctorservice.repository.DoctorSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorSlotRepository slotRepository;

    @Override
    @Transactional
    public Doctor createDoctor(Doctor doctor) {
        if (doctorRepository.existsByEmail(doctor.getEmail())) {
            throw new DuplicateDoctorEmailException(doctor.getEmail());
        }
        doctor.setId(null);
        doctor.setVerified(false);
        doctor.setActive(false);
        return doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public Doctor createApplication(Doctor doctor) {
        if (doctorRepository.existsByUserId(doctor.getUserId())) {
            throw new IllegalStateException("A doctor application already exists for this account");
        }
        if (doctorRepository.existsByEmail(doctor.getEmail())) {
            throw new DuplicateDoctorEmailException(doctor.getEmail());
        }
        if (doctorRepository.existsByLicenseNumber(doctor.getLicenseNumber())) {
            throw new IllegalStateException("A doctor application already exists for this license number");
        }
        doctor.setId(null);
        doctor.setVerified(false);
        doctor.setActive(false);
        return doctorRepository.save(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Doctor> getApplicationByUserId(UUID userId) {
        return doctorRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> getPendingApplications() {
        return doctorRepository.findByVerifiedFalseOrderByCreatedAtAsc();
    }

    @Override
    @Transactional
    public Doctor approveApplication(UUID id) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(() -> new DoctorNotFoundException(id));
        if (doctor.getUserId() == null) {
            throw new IllegalStateException("Doctor application is not linked to a user account");
        }
        doctor.setVerified(true);
        doctor.setActive(true);
        return doctorRepository.save(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Doctor getDoctorById(UUID id) {
        return doctorRepository.findById(id).orElseThrow(() -> new DoctorNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> getAllDoctors(String specialization, String department) {
        if (specialization != null && !specialization.isBlank()) {
            return doctorRepository.findByVerifiedTrueAndIsActiveTrueAndSpecializationIgnoreCase(specialization);
        } else if (department != null && !department.isBlank()) {
            return doctorRepository.findByVerifiedTrueAndIsActiveTrueAndDepartmentIgnoreCase(department);
        } else {
            return doctorRepository.findByVerifiedTrueAndIsActiveTrue();
        }
    }

    @Override
    @Transactional
    public List<DoctorSlot> createSlots(UUID doctorId, List<DoctorSlot> slots) {
        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new DoctorNotFoundException(doctorId));
        for (DoctorSlot slot : slots) {
            slot.setDoctor(doctor);
            slot.setStatus(SlotStatus.AVAILABLE);
        }
        return slotRepository.saveAll(slots);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorSlot> getSlotsByDate(UUID doctorId, LocalDate date) {
        // Verify doctor exists
        doctorRepository.findById(doctorId)
            .orElseThrow(() -> new DoctorNotFoundException(doctorId));

        if (date == null) {
            return slotRepository.findByDoctorIdOrderByStartTimeAsc(doctorId);
        }

        return slotRepository.findByDoctorIdAndDateOrderByStartTimeAsc(doctorId, date);
    }

    @Override
    @Transactional
    public Doctor linkUser(UUID doctorId, UUID userId) {
        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new DoctorNotFoundException(doctorId));
        doctor.setUserId(userId);
        return doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public Doctor updateDoctor(UUID id, Doctor doctorRequest) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(() -> new DoctorNotFoundException(id));
        if (doctorRequest.getName() != null) doctor.setName(doctorRequest.getName());
        if (doctorRequest.getEmail() != null) doctor.setEmail(doctorRequest.getEmail());
        if (doctorRequest.getSpecialization() != null) doctor.setSpecialization(doctorRequest.getSpecialization());
        if (doctorRequest.getPhone() != null) doctor.setPhone(doctorRequest.getPhone());
        if (doctorRequest.getLicenseNumber() != null) doctor.setLicenseNumber(doctorRequest.getLicenseNumber());
        if (doctorRequest.getDepartment() != null) doctor.setDepartment(doctorRequest.getDepartment());
        doctor.setYearsOfExperience(doctorRequest.getYearsOfExperience());
        return doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public Doctor verifyDoctor(UUID id, boolean verified) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(() -> new DoctorNotFoundException(id));
        doctor.setVerified(verified);
        doctor.setActive(verified);
        return doctorRepository.save(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Doctor getAvailableSlotsForAppointment(UUID doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new DoctorNotFoundException(doctorId));
        if (!doctor.isVerified() || !doctor.isActive()) {
            throw new DoctorNotFoundException(doctorId);
        }
        LocalDate effectiveDate = (date != null) ? date : LocalDate.now();
        List<DoctorSlot> availableSlots = slotRepository.findAvailableByDoctorAndDate(doctorId, effectiveDate);
        doctor.setSlots(availableSlots);
        return doctor;
    }
}
