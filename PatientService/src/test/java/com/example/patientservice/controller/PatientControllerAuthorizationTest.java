package com.example.patientservice.controller;

import com.example.patientservice.dto.TokenValidationResponse;
import com.example.patientservice.exception.ForbiddenException;
import com.example.patientservice.model.Patient;
import com.example.patientservice.service.PatientService;
import com.example.patientservice.util.AuthHelper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientControllerAuthorizationTest {
    private final PatientService patients = mock(PatientService.class);
    private final AuthHelper auth = mock(AuthHelper.class);
    private final PatientController controller = new PatientController(patients, auth);

    @Test
    void patientCannotReadAnotherPatientsProfile() {
        UUID profileId = UUID.randomUUID();
        when(auth.requireAuthenticated("Bearer token")).thenReturn(TokenValidationResponse.builder()
                .valid(true).userId(UUID.randomUUID()).role("PATIENT").build());
        when(patients.getPatientById(profileId)).thenReturn(
                Patient.builder().id(profileId).userId(UUID.randomUUID()).build());

        assertThrows(ForbiddenException.class,
                () -> controller.getPatientById("Bearer token", profileId));
    }

    @Test
    void ownerAndAuthorizedStaffCanReadProfile() {
        UUID profileId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Patient profile = Patient.builder().id(profileId).userId(ownerId).build();
        when(patients.getPatientById(profileId)).thenReturn(profile);
        when(auth.requireAuthenticated("Bearer owner")).thenReturn(TokenValidationResponse.builder()
                .valid(true).userId(ownerId).role("PATIENT").build());
        when(auth.requireAuthenticated("Bearer receptionist")).thenReturn(TokenValidationResponse.builder()
                .valid(true).userId(UUID.randomUUID()).role("RECEPTIONIST").build());

        assertSame(profile, controller.getPatientById("Bearer owner", profileId).getBody());
        assertSame(profile, controller.getPatientById("Bearer receptionist", profileId).getBody());
    }
}
