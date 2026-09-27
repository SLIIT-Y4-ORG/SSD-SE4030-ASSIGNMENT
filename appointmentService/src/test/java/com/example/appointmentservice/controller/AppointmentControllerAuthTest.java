package com.example.appointmentservice.controller;

import com.example.appointmentservice.client.DoctorServiceClient;
import com.example.appointmentservice.client.PatientServiceClient;
import com.example.appointmentservice.dto.CreateAppointmentRequest;
import com.example.appointmentservice.dto.TokenValidationResponse;
import com.example.appointmentservice.exception.UnauthorizedException;
import com.example.appointmentservice.service.AppointmentService;
import com.example.appointmentservice.util.AuthHelper;
import com.example.appointmentservice.util.InternalAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentController: Authorization header enforcement")
class AppointmentControllerAuthTest {

    private static final String VALID_TOKEN = "Bearer test-token-abc123";

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private AuthHelper authHelper;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private DoctorServiceClient doctorServiceClient;

    @Mock
    private InternalAuthService internalAuthService;

    private AppointmentController controller;
    private CreateAppointmentRequest validRequest;

    @BeforeEach
    void setup() {
        controller = new AppointmentController(
                appointmentService, authHelper, patientServiceClient, doctorServiceClient, internalAuthService);

        validRequest = new CreateAppointmentRequest();
        validRequest.setPatientId(UUID.randomUUID());
        validRequest.setDoctorId(UUID.randomUUID());
        validRequest.setSlotId(UUID.randomUUID());
        validRequest.setReason("Checkup");
        validRequest.setAmount(new BigDecimal("50.00"));
        validRequest.setCurrency("USD");
    }

    // ── 1. Missing (null) header ───────────────────────────────────────────────

    @Test
    @DisplayName("Missing Authorization header → UnauthorizedException, service not called")
    void missingHeaderThrowsUnauthorizedAndServiceNotCalled() {
        when(authHelper.requireRole(null, "PATIENT"))
                .thenThrow(new UnauthorizedException("Authentication required"));

        assertThatThrownBy(() -> controller.createAppointment(null, validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");

        // Service (and thus all downstream clients) is never reached
        verify(appointmentService, never()).createAppointment(any(), any());
        verify(authHelper).requireRole(null, "PATIENT");
    }

    // ── 2. Blank header ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Blank Authorization header → UnauthorizedException, service not called")
    void blankHeaderThrowsUnauthorizedAndServiceNotCalled() {
        when(authHelper.requireRole("   ", "PATIENT"))
                .thenThrow(new UnauthorizedException("Authentication required"));

        assertThatThrownBy(() -> controller.createAppointment("   ", validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");

        verify(appointmentService, never()).createAppointment(any(), any());
        verify(authHelper).requireRole("   ", "PATIENT");
    }

    @Test
    @DisplayName("Empty Authorization header → UnauthorizedException, service not called")
    void emptyHeaderThrowsUnauthorizedAndServiceNotCalled() {
        when(authHelper.requireRole("", "PATIENT"))
                .thenThrow(new UnauthorizedException("Authentication required"));

        assertThatThrownBy(() -> controller.createAppointment("", validRequest))
                .isInstanceOf(UnauthorizedException.class);

        verify(appointmentService, never()).createAppointment(any(), any());
        verify(authHelper).requireRole("", "PATIENT");
    }

    // ── 3. Non-Bearer scheme ──────────────────────────────────────────────────

    @Test
    @DisplayName("Basic auth header → UnauthorizedException, service not called")
    void basicAuthHeaderThrowsUnauthorizedAndServiceNotCalled() {
        when(authHelper.requireRole("Basic dXNlcjpwYXNz", "PATIENT"))
                .thenThrow(new UnauthorizedException("Authentication required"));

        assertThatThrownBy(() -> controller.createAppointment("Basic dXNlcjpwYXNz", validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");

        verify(appointmentService, never()).createAppointment(any(), any());
        verify(authHelper).requireRole("Basic dXNlcjpwYXNz", "PATIENT");
    }

    @Test
    @DisplayName("Plain token without Bearer → UnauthorizedException, service not called")
    void noSchemeHeaderThrowsUnauthorized() {
        when(authHelper.requireRole("raw-token-value", "PATIENT"))
                .thenThrow(new UnauthorizedException("Authentication required"));

        assertThatThrownBy(() -> controller.createAppointment("raw-token-value", validRequest))
                .isInstanceOf(UnauthorizedException.class);

        verify(appointmentService, never()).createAppointment(any(), any());
        verify(authHelper).requireRole("raw-token-value", "PATIENT");
    }

    // ── 4. "Bearer " with no token ────────────────────────────────────────────

    @Test
    @DisplayName("\"Bearer \" with no token → still rejected by userService returning null")
    void bearerWithNoTokenThrowsUnauthorized() {
        when(authHelper.requireRole(eq("Bearer "), eq("PATIENT")))
                .thenThrow(new UnauthorizedException("Authentication required"));

        assertThatThrownBy(() -> controller.createAppointment("Bearer ", validRequest))
                .isInstanceOf(UnauthorizedException.class);

        verify(appointmentService, never()).createAppointment(any(), any());
    }

    // ── 5. Valid token passes to service ─────────────────────────────────────

    @Test
    @DisplayName("Valid Bearer token → service.createAppointment() is called")
    void validTokenCallsService() {
        TokenValidationResponse fakeUser = new TokenValidationResponse();
        fakeUser.setValid(true);
        fakeUser.setUserId(UUID.randomUUID());
        fakeUser.setRole("PATIENT");
        when(authHelper.requireRole(eq(VALID_TOKEN), eq("PATIENT"))).thenReturn(fakeUser);
        // Service returns null — ResponseEntity.ok(null) is fine for this test scope
        when(appointmentService.createAppointment(any(), any())).thenReturn(null);

        controller.createAppointment(VALID_TOKEN, validRequest);

        verify(patientServiceClient).getPatientById(validRequest.getPatientId(), VALID_TOKEN);
        verify(appointmentService).createAppointment(validRequest, VALID_TOKEN);
    }

    // ── 6. Exception message safety ───────────────────────────────────────────

    @Test
    @DisplayName("UnauthorizedException message is the static safe string, not the header value")
    void exceptionMessageDoesNotEchoHeaderValue() {
        when(authHelper.requireRole("Basic leakedValue", "PATIENT"))
                .thenThrow(new UnauthorizedException("Authentication required"));

        assertThatThrownBy(() -> controller.createAppointment("Basic leakedValue", validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(Throwable::getMessage)
                .asString()
                .isEqualTo("Authentication required")
                .doesNotContain("leakedValue")
                .doesNotContain("Basic");
    }
}
