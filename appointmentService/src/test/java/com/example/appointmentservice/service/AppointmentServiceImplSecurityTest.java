package com.example.appointmentservice.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import com.example.appointmentservice.client.DoctorServiceClient;
import com.example.appointmentservice.client.PatientServiceClient;
import com.example.appointmentservice.client.PaymentServiceClient;
import com.example.appointmentservice.dto.CreateAppointmentRequest;
import com.example.appointmentservice.dto.DoctorDto;
import com.example.appointmentservice.dto.PatientDto;
import com.example.appointmentservice.exception.DownstreamDependencyException;
import com.example.appointmentservice.exception.ResourceNotFoundException;
import com.example.appointmentservice.exception.ServiceUnavailableException;
import com.example.appointmentservice.repository.AppointmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * V-03 (CWE-209) + Token-propagation fix: AppointmentServiceImpl security tests.
 *
 * Verifies that:
 * VI. AppointmentServiceImpl passes the Authorization header to PatientServiceClient.
 * VII. AppointmentServiceImpl passes the Authorization header to DoctorServiceClient.
 * A.  Genuine null from PatientServiceClient → ResourceNotFoundException (404).
 * B.  DownstreamDependencyException from PatientServiceClient → ServiceUnavailableException (503).
 * C.  ServiceUnavailableException message contains only the static safe string.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("V-03 + Token propagation: AppointmentServiceImpl security tests")
class AppointmentServiceImplSecurityTest {

    private static final String AUTH_HEADER = "Bearer test-token-abc123";

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PaymentServiceClient paymentServiceClient;
    @Mock
    private PatientServiceClient patientServiceClient;
    @Mock
    private DoctorServiceClient doctorServiceClient;

    @InjectMocks
    private AppointmentServiceImpl service;

    private CreateAppointmentRequest validRequest;

    @BeforeEach
    void setup() {
        validRequest = new CreateAppointmentRequest();
        validRequest.setPatientId(UUID.randomUUID());
        validRequest.setDoctorId(UUID.randomUUID());
        validRequest.setSlotId(UUID.randomUUID());
        validRequest.setReason("Checkup");
        validRequest.setAmount(new BigDecimal("100.00"));
        validRequest.setCurrency("USD");

        when(appointmentRepository.existsBySlotIdAndStatus(any(), any())).thenReturn(false);
    }

    // ── Test VI: Authorization header is passed to PatientServiceClient ────────

    @Test
    @DisplayName("AppointmentServiceImpl passes Authorization header to PatientServiceClient")
    void passesAuthHeaderToPatientServiceClient() {
        when(patientServiceClient.getPatientById(any(), eq(AUTH_HEADER)))
                .thenReturn(new PatientDto());
        when(doctorServiceClient.getDoctorById(any(), eq(AUTH_HEADER)))
                .thenReturn(null); // triggers ResourceNotFoundException, but header was forwarded

        try {
            service.createAppointment(validRequest, AUTH_HEADER);
        } catch (Exception ignored) { /* not testing the full flow here */ }

        verify(patientServiceClient).getPatientById(validRequest.getPatientId(), AUTH_HEADER);
    }

    // ── Test VII: Authorization header is passed to DoctorServiceClient ────────

    @Test
    @DisplayName("AppointmentServiceImpl passes Authorization header to DoctorServiceClient")
    void passesAuthHeaderToDoctorServiceClient() {
        when(patientServiceClient.getPatientById(any(), eq(AUTH_HEADER)))
                .thenReturn(new PatientDto());
        when(doctorServiceClient.getDoctorById(any(), eq(AUTH_HEADER)))
                .thenReturn(null); // triggers ResourceNotFoundException

        try {
            service.createAppointment(validRequest, AUTH_HEADER);
        } catch (Exception ignored) { /* not testing the full flow here */ }

        verify(doctorServiceClient).getDoctorById(validRequest.getDoctorId(), AUTH_HEADER);
    }

    // ── Test A: Genuine 404 (patient does not exist) ───────────────────────────

    @Test
    @DisplayName("Genuine patient 404 (null return) produces ResourceNotFoundException")
    void genuinePatient404ProducesResourceNotFound() {
        when(patientServiceClient.getPatientById(any(), any())).thenReturn(null);

        assertThatThrownBy(() -> service.createAppointment(validRequest, AUTH_HEADER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    @DisplayName("Genuine doctor 404 (null return) produces ResourceNotFoundException")
    void genuineDoctor404ProducesResourceNotFound() {
        when(patientServiceClient.getPatientById(any(), any())).thenReturn(new PatientDto());
        when(doctorServiceClient.getDoctorById(any(), any())).thenReturn(null);

        assertThatThrownBy(() -> service.createAppointment(validRequest, AUTH_HEADER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor not found");
    }

    // ── Test B: DownstreamDependencyException → ServiceUnavailableException ────

    @Test
    @DisplayName("Patient service connection failure → ServiceUnavailableException")
    void patientConnectionFailureProducesServiceUnavailable() {
        DownstreamDependencyException downstreamEx = new DownstreamDependencyException(
                "Patient service unavailable",
                new ResourceAccessException("Connection refused: http://patient-service:8080"));

        when(patientServiceClient.getPatientById(any(), any())).thenThrow(downstreamEx);

        assertThatThrownBy(() -> service.createAppointment(validRequest, AUTH_HEADER))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Unable to validate patient at this time");
    }

    @Test
    @DisplayName("Doctor service connection failure → ServiceUnavailableException")
    void doctorConnectionFailureProducesServiceUnavailable() {
        when(patientServiceClient.getPatientById(any(), any())).thenReturn(new PatientDto());
        DownstreamDependencyException downstreamEx = new DownstreamDependencyException(
                "Doctor service unavailable",
                new ResourceAccessException("Connection refused: http://doctor-service:8082"));

        when(doctorServiceClient.getDoctorById(any(), any())).thenThrow(downstreamEx);

        assertThatThrownBy(() -> service.createAppointment(validRequest, AUTH_HEADER))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Unable to validate doctor at this time");
    }

    // ── Test C: ServiceUnavailableException message contains no internal data ──

    @Test
    @DisplayName("ServiceUnavailableException message does not contain hostname, port or exception details")
    void serviceUnavailableMessageDoesNotLeakInternals() {
        DownstreamDependencyException downstreamEx = new DownstreamDependencyException(
                "Patient service unavailable",
                new ResourceAccessException(
                        "I/O error on GET http://patient-service:8080/api/patients/123 "
                        + "500 Internal Server Error: {\"db\": \"connection failed\"}"));

        when(patientServiceClient.getPatientById(any(), any())).thenThrow(downstreamEx);

        ServiceUnavailableException thrown = (ServiceUnavailableException)
                org.junit.jupiter.api.Assertions.assertThrows(
                        ServiceUnavailableException.class,
                        () -> service.createAppointment(validRequest, AUTH_HEADER));

        String msg = thrown.getMessage();
        assertThat(msg).doesNotContain("patient-service");
        assertThat(msg).doesNotContain("8080");
        assertThat(msg).doesNotContain("http://");
        assertThat(msg).doesNotContain("Connection refused");
        assertThat(msg).doesNotContain("Internal Server Error");
        assertThat(msg).doesNotContain("connection failed");
    }
}
