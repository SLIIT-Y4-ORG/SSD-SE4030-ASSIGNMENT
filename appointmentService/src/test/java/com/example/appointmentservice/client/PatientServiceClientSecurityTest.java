package com.example.appointmentservice.client;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.appointmentservice.dto.PatientDto;
import com.example.appointmentservice.exception.DownstreamDependencyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * V-03 (CWE-209) + Token-propagation fix:
 * PatientServiceClient security and integration tests.
 *
 * Verifies that:
 * I.  The Authorization header is forwarded verbatim to Patient Service.
 * II. The token is NOT included in client-facing exception messages.
 * A.  A genuine HTTP 404 response returns null without throwing.
 * B.  A connection/ResourceAccessException is converted to DownstreamDependencyException
 *     with cause preserved and no internal message leaked.
 * C.  A downstream HTTP 500 is also converted to DownstreamDependencyException.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("V-03 + Token propagation: PatientServiceClient security tests")
class PatientServiceClientSecurityTest {

    private static final String AUTH_HEADER = "Bearer test-token-abc123";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PatientServiceClient client;

    private final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(client, "patientServiceUrl", "http://patient-service:8080");
    }

    // ── Test I: Authorization header is forwarded ──────────────────────────────

    @Test
    @DisplayName("Authorization header is forwarded to Patient Service verbatim")
    void authorizationHeaderIsForwardedToPatientService() {
        PatientDto expectedPatient = new PatientDto();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PatientDto.class)))
                .thenReturn(ResponseEntity.ok(expectedPatient));

        client.getPatientById(patientId, AUTH_HEADER);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), entityCaptor.capture(), eq(PatientDto.class));

        String sentHeader = entityCaptor.getValue().getHeaders().getFirst("Authorization");
        assertThat(sentHeader).isEqualTo(AUTH_HEADER);
    }

    @Test
    @DisplayName("Correct patient is returned when auth succeeds")
    void returnsPatientWhenAuthSucceeds() {
        PatientDto expectedPatient = new PatientDto();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PatientDto.class)))
                .thenReturn(ResponseEntity.ok(expectedPatient));

        PatientDto result = client.getPatientById(patientId, AUTH_HEADER);

        assertThat(result).isSameAs(expectedPatient);
    }

    // ── Test II: Token not in exception messages ───────────────────────────────

    @Test
    @DisplayName("Bearer token is not included in DownstreamDependencyException message")
    void tokenNotInExceptionMessage() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PatientDto.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> client.getPatientById(patientId, AUTH_HEADER))
                .isInstanceOf(DownstreamDependencyException.class)
                .extracting(Throwable::getMessage)
                .asString()
                .doesNotContain("test-token-abc123")
                .doesNotContain("Bearer");
    }

    // ── Test A: Genuine 404 ────────────────────────────────────────────────────

    @Test
    @DisplayName("Genuine HTTP 404 returns null without exception")
    void genuine404ReturnsNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PatientDto.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        PatientDto result = client.getPatientById(patientId, AUTH_HEADER);

        assertThat(result).isNull();
    }

    // ── Test B: Connection / ResourceAccessException ───────────────────────────

    @Test
    @DisplayName("Connection failure throws DownstreamDependencyException with original cause")
    void connectionFailureThrowsDownstreamException() {
        ResourceAccessException cause = new ResourceAccessException(
                "I/O error on GET request for \"http://patient-service:8080/api/patients/"
                + patientId + "\": Connection refused");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PatientDto.class)))
                .thenThrow(cause);

        assertThatThrownBy(() -> client.getPatientById(patientId, AUTH_HEADER))
                .isInstanceOf(DownstreamDependencyException.class)
                .hasCause(cause)
                // The message is a static safe string — never ex.getMessage()
                .hasMessage("Patient service unavailable");
    }

    @Test
    @DisplayName("DownstreamDependencyException message does not contain internal URL or hostname")
    void downstreamExceptionMessageDoesNotLeakInternals() {
        ResourceAccessException cause = new ResourceAccessException(
                "Connection refused to http://patient-service:8080/api/patients/" + patientId);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PatientDto.class)))
                .thenThrow(cause);

        DownstreamDependencyException thrown = (DownstreamDependencyException)
                org.junit.jupiter.api.Assertions.assertThrows(
                        DownstreamDependencyException.class,
                        () -> client.getPatientById(patientId, AUTH_HEADER));

        String msg = thrown.getMessage();
        assertThat(msg).doesNotContain("patient-service");
        assertThat(msg).doesNotContain("8080");
        assertThat(msg).doesNotContain("http://");
        assertThat(msg).doesNotContain("Connection refused");
    }

    // ── Test C: Downstream HTTP 500 ────────────────────────────────────────────

    @Test
    @DisplayName("Downstream HTTP 500 with sensitive body throws DownstreamDependencyException, not RuntimeException")
    void downstream500ThrowsDownstreamException() {
        String sensitiveBody = "{\"error\":\"database connection failed\",\"url\":\"http://patient-service:8080/internal\"}";
        RestClientException serverError = new HttpClientErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                sensitiveBody);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PatientDto.class)))
                .thenThrow(serverError);

        assertThatThrownBy(() -> client.getPatientById(patientId, AUTH_HEADER))
                .isInstanceOf(DownstreamDependencyException.class)
                .hasMessage("Patient service unavailable")
                .extracting(Throwable::getMessage)
                .asString()
                .doesNotContain("database connection failed")
                .doesNotContain("patient-service")
                .doesNotContain("8080")
                .doesNotContain("http://");
    }
}
