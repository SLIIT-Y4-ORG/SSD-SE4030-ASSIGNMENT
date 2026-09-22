package com.example.appointmentservice.client;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.appointmentservice.dto.DoctorDto;
import com.example.appointmentservice.exception.DownstreamDependencyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * V-03 (CWE-209): DoctorServiceClient security tests.
 *
 * Verifies that:
 * A. A genuine HTTP 404 response returns null without throwing.
 * B. A connection/ResourceAccessException is converted to DownstreamDependencyException
 *    with cause preserved and no internal message leaked.
 * C. A downstream HTTP 500 is also converted to DownstreamDependencyException.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("V-03: DoctorServiceClient security tests")
class DoctorServiceClientSecurityTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DoctorServiceClient client;

    private final UUID doctorId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(client, "doctorServiceUrl", "http://doctor-service:8082");
    }

    // ── Test A: Genuine 404 ────────────────────────────────────────────────────

    @Test
    @DisplayName("Genuine HTTP 404 returns null without exception")
    void genuine404ReturnsNull() {
        when(restTemplate.getForObject(anyString(), eq(DoctorDto.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        DoctorDto result = client.getDoctorById(doctorId);

        assertThat(result).isNull();
    }

    // ── Test B: Connection / ResourceAccessException ───────────────────────────

    @Test
    @DisplayName("Connection failure throws DownstreamDependencyException with original cause")
    void connectionFailureThrowsDownstreamException() {
        ResourceAccessException cause = new ResourceAccessException(
                "I/O error on GET request for \"http://doctor-service:8082/api/doctors/"
                + doctorId + "\": Connection refused");
        when(restTemplate.getForObject(anyString(), eq(DoctorDto.class))).thenThrow(cause);

        assertThatThrownBy(() -> client.getDoctorById(doctorId))
                .isInstanceOf(DownstreamDependencyException.class)
                .hasCause(cause)
                .hasMessage("Doctor service unavailable");
    }

    @Test
    @DisplayName("DownstreamDependencyException message does not contain internal URL or hostname")
    void downstreamExceptionMessageDoesNotLeakInternals() {
        ResourceAccessException cause = new ResourceAccessException(
                "Connection refused to http://doctor-service:8082/api/doctors/" + doctorId);
        when(restTemplate.getForObject(anyString(), eq(DoctorDto.class))).thenThrow(cause);

        DownstreamDependencyException thrown = (DownstreamDependencyException)
                org.junit.jupiter.api.Assertions.assertThrows(
                        DownstreamDependencyException.class,
                        () -> client.getDoctorById(doctorId));

        String msg = thrown.getMessage();
        assertThat(msg).doesNotContain("doctor-service");
        assertThat(msg).doesNotContain("8082");
        assertThat(msg).doesNotContain("http://");
        assertThat(msg).doesNotContain("Connection refused");
    }

    // ── Test C: Downstream HTTP 500 ────────────────────────────────────────────

    @Test
    @DisplayName("Downstream HTTP 500 with sensitive body throws DownstreamDependencyException")
    void downstream500ThrowsDownstreamException() {
        String sensitiveBody = "{\"error\":\"NullPointerException at DoctorController:42\","
                + "\"url\":\"http://doctor-service:8082/internal\"}";
        RestClientException serverError = new HttpClientErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR, sensitiveBody);
        when(restTemplate.getForObject(anyString(), eq(DoctorDto.class))).thenThrow(serverError);

        assertThatThrownBy(() -> client.getDoctorById(doctorId))
                .isInstanceOf(DownstreamDependencyException.class)
                .hasMessage("Doctor service unavailable")
                .extracting(Throwable::getMessage)
                .asString()
                .doesNotContain("doctor-service")
                .doesNotContain("8082")
                .doesNotContain("http://")
                .doesNotContain("NullPointerException");
    }
}
