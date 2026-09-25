package com.example.appointmentservice.controller;

import com.example.appointmentservice.dto.CreateAppointmentRequest;
import com.example.appointmentservice.exception.UnauthorizedException;
import com.example.appointmentservice.security.AuthorizationHeaderValidator;
import com.example.appointmentservice.service.AppointmentService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for Authorization header enforcement in {@link AppointmentController}.
 *
 * The controller method is called directly (not via MockMvc) because
 * {@code @WebMvcTest} is not available in this module's test classpath.
 * HTTP status mapping (UnauthorizedException → 401) is covered separately by
 * {@link com.example.appointmentservice.exception.GlobalExceptionHandlerSecurityTest}.
 *
 * Verifies that:
 * 1. Missing (null) header → UnauthorizedException before service is called.
 * 2. Blank header → UnauthorizedException before service is called.
 * 3. Non-Bearer header → UnauthorizedException before service is called.
 * 4. "Bearer " with no token → UnauthorizedException before service is called.
 * 5. Valid Bearer token → service.createAppointment() is called.
 * 6. Neither PatientServiceClient nor DoctorServiceClient is invoked on rejection
 *    (they are not reachable via the controller — the service mock is never called).
 *
 * All token values are dummies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentController: Authorization header enforcement")
class AppointmentControllerAuthTest {

    private static final String VALID_TOKEN = "Bearer test-token-abc123";

    @Mock
    private AppointmentService appointmentService;

    // The real validator is used — not a mock — so its logic is exercised.
    private final AuthorizationHeaderValidator validator = new AuthorizationHeaderValidator();

    private AppointmentController controller;
    private CreateAppointmentRequest validRequest;

    @BeforeEach
    void setup() {
        controller = new AppointmentController(appointmentService, validator);

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
        assertThatThrownBy(() -> controller.createAppointment(null, validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");

        // Service (and thus all downstream clients) is never reached
        verify(appointmentService, never()).createAppointment(any(), any());
    }

    // ── 2. Blank header ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Blank Authorization header → UnauthorizedException, service not called")
    void blankHeaderThrowsUnauthorizedAndServiceNotCalled() {
        assertThatThrownBy(() -> controller.createAppointment("   ", validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");

        verify(appointmentService, never()).createAppointment(any(), any());
    }

    @Test
    @DisplayName("Empty Authorization header → UnauthorizedException, service not called")
    void emptyHeaderThrowsUnauthorizedAndServiceNotCalled() {
        assertThatThrownBy(() -> controller.createAppointment("", validRequest))
                .isInstanceOf(UnauthorizedException.class);

        verify(appointmentService, never()).createAppointment(any(), any());
    }

    // ── 3. Non-Bearer scheme ──────────────────────────────────────────────────

    @Test
    @DisplayName("Basic auth header → UnauthorizedException, service not called")
    void basicAuthHeaderThrowsUnauthorizedAndServiceNotCalled() {
        assertThatThrownBy(() -> controller.createAppointment("Basic dXNlcjpwYXNz", validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");

        verify(appointmentService, never()).createAppointment(any(), any());
    }

    @Test
    @DisplayName("Plain token without Bearer → UnauthorizedException, service not called")
    void noSchemeHeaderThrowsUnauthorized() {
        assertThatThrownBy(() -> controller.createAppointment("raw-token-value", validRequest))
                .isInstanceOf(UnauthorizedException.class);

        verify(appointmentService, never()).createAppointment(any(), any());
    }

    // ── 4. "Bearer " with no token ────────────────────────────────────────────

    @Test
    @DisplayName("\"Bearer \" with no token → UnauthorizedException, service not called")
    void bearerWithNoTokenThrowsUnauthorized() {
        assertThatThrownBy(() -> controller.createAppointment("Bearer ", validRequest))
                .isInstanceOf(UnauthorizedException.class);

        verify(appointmentService, never()).createAppointment(any(), any());
    }

    // ── 5. Valid token passes to service ─────────────────────────────────────

    @Test
    @DisplayName("Valid Bearer token → service.createAppointment() is called")
    void validTokenCallsService() {
        // Service returns null — ResponseEntity.ok(null) is fine for this test scope
        org.mockito.Mockito.when(appointmentService.createAppointment(any(), any())).thenReturn(null);

        controller.createAppointment(VALID_TOKEN, validRequest);

        verify(appointmentService).createAppointment(validRequest, VALID_TOKEN);
    }

    // ── 6. Exception message safety ───────────────────────────────────────────

    @Test
    @DisplayName("UnauthorizedException message is the static safe string, not the header value")
    void exceptionMessageDoesNotEchoHeaderValue() {
        assertThatThrownBy(() -> controller.createAppointment("Basic leakedValue", validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(Throwable::getMessage)
                .asString()
                .isEqualTo("Authentication required")
                .doesNotContain("leakedValue")
                .doesNotContain("Basic");
    }
}
