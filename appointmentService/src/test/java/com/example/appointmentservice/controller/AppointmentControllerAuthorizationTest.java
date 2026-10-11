package com.example.appointmentservice.controller;

import com.example.appointmentservice.client.DoctorServiceClient;
import com.example.appointmentservice.client.PatientServiceClient;
import com.example.appointmentservice.dto.AppointmentResponse;
import com.example.appointmentservice.dto.TokenValidationResponse;
import com.example.appointmentservice.exception.ForbiddenException;
import com.example.appointmentservice.service.AppointmentService;
import com.example.appointmentservice.util.AuthHelper;
import com.example.appointmentservice.util.InternalAuthService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AppointmentControllerAuthorizationTest {
    private final AppointmentService appointments = mock(AppointmentService.class);
    private final AuthHelper auth = mock(AuthHelper.class);
    private final PatientServiceClient patients = mock(PatientServiceClient.class);
    private final DoctorServiceClient doctors = mock(DoctorServiceClient.class);
    private final InternalAuthService internalAuth = mock(InternalAuthService.class);
    private final AppointmentController controller = new AppointmentController(
            appointments, auth, patients, doctors, internalAuth);

    @Test
    void unrelatedRoleCannotReadAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(appointments.getAppointmentById(appointmentId)).thenReturn(AppointmentResponse.builder()
                .id(appointmentId).patientId(UUID.randomUUID()).doctorId(UUID.randomUUID()).build());
        when(auth.requireAuthenticated("Bearer token")).thenReturn(new TokenValidationResponse());

        assertThrows(ForbiddenException.class,
                () -> controller.getAppointmentById("Bearer token", appointmentId));
    }

    @Test
    void paymentCallbackRequiresInternalKey() {
        doThrow(new com.example.appointmentservice.exception.UnauthorizedException("Invalid internal API key"))
                .when(internalAuth).verify("bad-key");

        assertThrows(com.example.appointmentservice.exception.UnauthorizedException.class,
                () -> controller.handlePaymentCallback("bad-key", java.util.Map.of()));
        verifyNoInteractions(appointments);
    }
}
