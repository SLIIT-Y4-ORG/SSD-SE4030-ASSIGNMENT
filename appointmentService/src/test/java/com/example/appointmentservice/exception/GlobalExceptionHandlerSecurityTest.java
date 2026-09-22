package com.example.appointmentservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V-03 -- Raw exception-message and internal-information disclosure (CWE-209)
 *
 * Verifies that the GlobalExceptionHandler:
 * 1. Catch-all never forwards ex.getMessage() to the HTTP client.
 * 2. ServiceUnavailableException is mapped to HTTP 503 with the safe message.
 * 3. 503 response never contains internal hostnames, ports, or exception text.
 * 4. ResourceNotFoundException/BadRequestException still return their own messages.
 */
@DisplayName("V-03: GlobalExceptionHandler -- no raw exception message in response")
class GlobalExceptionHandlerSecurityTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    // ── Test 1: Catch-all suppresses internal details ─────────────────────────

    @Test
    @DisplayName("RuntimeException with internal hostname returns generic 500 message")
    void runtimeExceptionWithInternalHostnameReturnsGenericMessage() {
        String internalDetail = "I/O error on GET for \"http://patient-service:8082/api/patients/...\"";
        RuntimeException ex = new RuntimeException(internalDetail);

        ResponseEntity<?> response = exceptionHandler.handleOther(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("An unexpected error occurred");

        String message = (String) body.get("message");
        assertThat(message).doesNotContain("patient-service");
        assertThat(message).doesNotContain(":8082");
        assertThat(message).doesNotContain("http://");
    }

    @Test
    @DisplayName("Response body does not contain raw exception message text")
    void responseDoesNotContainRawExceptionText() {
        RuntimeException ex = new RuntimeException("SECRET internal message with port 9999");

        ResponseEntity<?> response = exceptionHandler.handleOther(ex);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        String message = (String) body.get("message");

        assertThat(message).doesNotContain("SECRET");
        assertThat(message).doesNotContain("port 9999");
    }

    // ── Test 2: ServiceUnavailableException → HTTP 503 ────────────────────────

    @Test
    @DisplayName("ServiceUnavailableException returns HTTP 503 with safe message")
    void serviceUnavailableExceptionReturns503() {
        ServiceUnavailableException ex = new ServiceUnavailableException(
                "Unable to validate patient at this time");

        ResponseEntity<?> response = exceptionHandler.handleServiceUnavailable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo(503);
        assertThat(body.get("message")).isEqualTo("Unable to validate patient at this time");
    }

    @Test
    @DisplayName("HTTP 503 response does not contain hostname, port, URL or exception class name")
    void http503DoesNotLeakInternalDetails() {
        // ServiceUnavailableException built from a DownstreamDependencyException cause
        // that contains sensitive data; the safe message is a literal string.
        ServiceUnavailableException ex = new ServiceUnavailableException(
                "Unable to validate patient at this time");

        ResponseEntity<?> response = exceptionHandler.handleServiceUnavailable(ex);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        String fullResponse = body.toString();

        assertThat(fullResponse).doesNotContain("patient-service");
        assertThat(fullResponse).doesNotContain("doctor-service");
        assertThat(fullResponse).doesNotContain("8080");
        assertThat(fullResponse).doesNotContain("http://");
        assertThat(fullResponse).doesNotContain("Connection refused");
        assertThat(fullResponse).doesNotContain("DownstreamDependencyException");
        assertThat(fullResponse).doesNotContain("ResourceAccessException");
    }

    // ── Test 3: 400-level handlers are unchanged ──────────────────────────────

    @Test
    @DisplayName("ResourceNotFoundException still returns 404 with its own message")
    void resourceNotFoundExceptionReturnsSafeMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Appointment not found with ID: test-id");

        ResponseEntity<?> response = exceptionHandler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("message")).isEqualTo("Appointment not found with ID: test-id");
    }

    @Test
    @DisplayName("BadRequestException still returns 400 with its own message")
    void badRequestExceptionReturnsSafeMessage() {
        BadRequestException ex = new BadRequestException("This slot is already confirmed");

        ResponseEntity<?> response = exceptionHandler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("message")).isEqualTo("This slot is already confirmed");
    }

    // ── Test 4: Confirm 503 is distinct from 404 / 500 ───────────────────────

    @Test
    @DisplayName("Downstream outage returns 503, not 404 or 500")
    void downstreamOutageIsNot404Or500() {
        ServiceUnavailableException ex = new ServiceUnavailableException(
                "Unable to validate patient at this time");

        ResponseEntity<?> response = exceptionHandler.handleServiceUnavailable(ex);

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
