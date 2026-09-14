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
 * Verifies that the GlobalExceptionHandler catch-all no longer forwards
 * ex.getMessage() to the HTTP client. Internal hostnames, ports, and other
 * runtime details must not appear in the response body.
 */
@DisplayName("V-03: GlobalExceptionHandler -- no raw exception message in response")
class GlobalExceptionHandlerSecurityTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    // ── Test 1: Internal hostname not disclosed ────────────────────────────────

    @Test
    @DisplayName("RuntimeException with internal hostname returns generic 500 message")
    void runtimeExceptionWithInternalHostnameReturnsGenericMessage() {
        // Simulate an inter-service call failure whose message contains an internal hostname
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

    // ── Test 2: 400-level handlers are unchanged ──────────────────────────────

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
}
