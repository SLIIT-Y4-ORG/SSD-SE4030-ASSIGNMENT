package com.example.doctorservice.controller;

import com.example.doctorservice.client.UserServiceClient;
import com.example.doctorservice.dto.DoctorApplicationRequest;
import com.example.doctorservice.dto.TokenValidationResponse;
import com.example.doctorservice.exception.ForbiddenException;
import com.example.doctorservice.model.Doctor;
import com.example.doctorservice.model.DoctorSlot;
import com.example.doctorservice.service.DoctorService;
import com.example.doctorservice.util.AuthHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorApplicationSecurityTest {
    private final DoctorService doctors = mock(DoctorService.class);
    private final AuthHelper auth = mock(AuthHelper.class);
    private final UserServiceClient users = mock(UserServiceClient.class);
    private final DoctorController controller = new DoctorController(doctors, auth, users);

    @Test
    void patientApplicationUsesAuthenticatedIdentityAndStartsPending() {
        UUID patientId = UUID.randomUUID();
        when(auth.requireRole("Bearer patient-token", "PATIENT")).thenReturn(
                TokenValidationResponse.builder().valid(true).userId(patientId)
                        .email("patient@example.com").role("PATIENT").build());
        when(doctors.createApplication(any(Doctor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.submitApplication("Bearer patient-token",
                new DoctorApplicationRequest("Applicant", "+94770000000", "Cardiology", "SLMC-123"));

        ArgumentCaptor<Doctor> submitted = ArgumentCaptor.forClass(Doctor.class);
        verify(doctors).createApplication(submitted.capture());
        assertEquals(patientId, submitted.getValue().getUserId());
        assertEquals("patient@example.com", submitted.getValue().getEmail());
        assertFalse(submitted.getValue().isVerified());
        assertFalse(submitted.getValue().isActive());
    }

    @Test
    void approvalPromotesLinkedUserBeforePublishingDoctor() {
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Doctor pending = Doctor.builder().id(doctorId).userId(userId).verified(false).isActive(false).build();
        Doctor approved = Doctor.builder().id(doctorId).userId(userId).verified(true).isActive(true).build();
        when(auth.requireRole("Bearer admin-token", "ADMIN")).thenReturn(
                TokenValidationResponse.builder().valid(true).role("ADMIN").build());
        when(auth.stripBearer("Bearer admin-token")).thenReturn("admin-token");
        when(doctors.getDoctorById(doctorId)).thenReturn(pending);
        when(doctors.approveApplication(doctorId)).thenReturn(approved);

        Doctor result = controller.approveApplication("Bearer admin-token", doctorId).getBody();

        assertNotNull(result);
        assertTrue(result.isVerified());
        verify(users).updateUserRole(userId, "DOCTOR", "admin-token");
        verify(doctors).approveApplication(doctorId);
    }

    @Test
    void failedProfileApprovalCompensatesRolePromotion() {
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(auth.requireRole("Bearer admin-token", "ADMIN")).thenReturn(
                TokenValidationResponse.builder().valid(true).role("ADMIN").build());
        when(auth.stripBearer("Bearer admin-token")).thenReturn("admin-token");
        when(doctors.getDoctorById(doctorId)).thenReturn(Doctor.builder().id(doctorId).userId(userId).build());
        when(doctors.approveApplication(doctorId)).thenThrow(new IllegalStateException("save failed"));

        assertThrows(IllegalStateException.class,
                () -> controller.approveApplication("Bearer admin-token", doctorId));

        verify(users).updateUserRole(userId, "DOCTOR", "admin-token");
        verify(users).updateUserRole(userId, "PATIENT", "admin-token");
    }

    @Test
    void doctorCannotManageAnotherDoctorsSchedule() {
        UUID authenticatedUserId = UUID.randomUUID();
        UUID otherDoctorId = UUID.randomUUID();
        when(auth.requireRole("Bearer doctor-token", "ADMIN", "DOCTOR", "RECEPTIONIST"))
                .thenReturn(TokenValidationResponse.builder().valid(true)
                        .userId(authenticatedUserId).role("DOCTOR").build());
        when(doctors.getDoctorById(otherDoctorId)).thenReturn(Doctor.builder()
                .id(otherDoctorId).userId(UUID.randomUUID()).verified(true).isActive(true).build());

        assertThrows(ForbiddenException.class, () -> controller.createSlots(
                "Bearer doctor-token", otherDoctorId, List.of(new DoctorSlot())));

        verify(doctors, never()).createSlots(any(), any());
    }
}
