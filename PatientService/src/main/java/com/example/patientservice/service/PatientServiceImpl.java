package com.example.patientservice.service;

import com.example.patientservice.exception.DuplicatePatientException;
import com.example.patientservice.exception.PatientNotFoundException;
import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    public Patient createPatient(Patient patient, UUID userId) {
        if (patientRepository.existsByEmail(patient.getEmail())) {
            throw new DuplicatePatientException("Patient with email " + patient.getEmail() + " already exists");
        }
        if (userId != null && patientRepository.existsByUserId(userId)) {
            throw new DuplicatePatientException("Patient profile already exists for this user");
        }
        patient.setUserId(userId);
        return patientRepository.save(patient);
    }

    @Override
    public Patient getPatientById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));
    }

    @Override
    public Patient getPatientByUserId(UUID userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientNotFoundException("Patient profile not found for user: " + userId));
    }

    @Override
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    @Override
    public Patient updatePatient(UUID id, Patient updated) {
        Patient existing = getPatientById(id);
        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
        if (updated.getDateOfBirth() != null) existing.setDateOfBirth(updated.getDateOfBirth());
        if (updated.getGender() != null) existing.setGender(updated.getGender());
        if (updated.getAddress() != null) existing.setAddress(updated.getAddress());
        if (updated.getBloodGroup() != null) existing.setBloodGroup(updated.getBloodGroup());
        if (updated.getAllergies() != null) existing.setAllergies(updated.getAllergies());
        return patientRepository.save(existing);
    }
}
