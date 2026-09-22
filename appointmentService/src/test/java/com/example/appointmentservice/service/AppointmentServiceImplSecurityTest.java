package com.example.appointmentservice.service;

import java.math.BigDecimal;
import java.util.Optional;
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
import com.example.appointmentservice.dto.PatientDto;
import com.example.appointmentservice.dto.DoctorDto;
import com.example.appointmentservice.exception.DownstreamDependencyException;
import com.example.appointmentservice.exception.ResourceNotFoundException;
import com.example.appointmentservice.exception.ServiceUnavailableException;
import com.example.appointmentservice.repository.AppointmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


/**
 * V-03 (CWE-209): AppointmentServiceImpl security tests.
 *
 * Verifies that:
 * A. Genuine null from PatientServiceClient → ResourceNotFoundException (404).
 * B. DownstreamDependencyException from PatientServiceClient → ServiceUnavailableException (503).
 * C. ServiceUnavailableException message contains only the static safe string.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("V-03: AppointmentServiceImpl security tests")
class AppointmentServiceImplSecurityTest {

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

    // ── Test A: Genuine 404 (patient does not exist) ───────────────────────────

    @Test
    @DisplayName("Genuine patient 404 (null return) produces ResourceNotFoundException")
    void genuinePatient404ProducesResourceNotFound() {
        when(patientServiceClient.getPatientById(any())).thenReturn(null);

        assertThatThrownBy(() -> service.createAppointment(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    @DisplayName("Genuine doctor 404 (null return) produces ResourceNotFoundException")
    void genuineDoctor404ProducesResourceNotFound() {
        when(patientServiceClient.getPatientById(any())).thenReturn(new PatientDto());
        when(doctorServiceClient.getDoctorById(any())).thenReturn(null);

        assertThatThrownBy(() -> service.createAppointment(validRequest))
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

        when(patientServiceClient.getPatientById(any())).thenThrow(downstreamEx);

        assertThatThrownBy(() -> service.createAppointment(validRequest))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Unable to validate patient at this time");
    }

    @Test
    @DisplayName("Doctor service connection failure → ServiceUnavailableException")
    void doctorConnectionFailureProducesServiceUnavailable() {
        when(patientServiceClient.getPatientById(any())).thenReturn(new PatientDto());
        DownstreamDependencyException downstreamEx = new DownstreamDependencyException(
                "Doctor service unavailable",
                new ResourceAccessException("Connection refused: http://doctor-service:8082"));

        when(doctorServiceClient.getDoctorById(any())).thenThrow(downstreamEx);

        assertThatThrownBy(() -> service.createAppointment(validRequest))
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

        when(patientServiceClient.getPatientById(any())).thenThrow(downstreamEx);

        ServiceUnavailableException thrown = (ServiceUnavailableException)
                org.junit.jupiter.api.Assertions.assertThrows(
                        ServiceUnavailableException.class,
                        () -> service.createAppointment(validRequest));

        String msg = thrown.getMessage();
        assertThat(msg).doesNotContain("patient-service");
        assertThat(msg).doesNotContain("8080");
        assertThat(msg).doesNotContain("http://");
        assertThat(msg).doesNotContain("Connection refused");
        assertThat(msg).doesNotContain("Internal Server Error");
        assertThat(msg).doesNotContain("connection failed");
    }

}
