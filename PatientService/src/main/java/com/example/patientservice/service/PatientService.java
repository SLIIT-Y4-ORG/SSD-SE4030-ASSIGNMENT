package com.example.patientservice.service;

import com.example.patientservice.model.Patient;

import java.util.List;
import java.util.UUID;

public interface PatientService {

    Patient createPatient(Patient patient, UUID userId);

    Patient getPatientById(UUID id);

    Patient getPatientByUserId(UUID userId);

    List<Patient> getAllPatients();

    Patient updatePatient(UUID id, Patient patient);
}
