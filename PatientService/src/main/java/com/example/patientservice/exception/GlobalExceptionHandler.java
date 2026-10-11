package com.example.patientservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePatientNotFound(PatientNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicatePatientException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePatient(DuplicatePatientException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Token-propagation fix: MissingRequestHeaderException is thrown by Spring
     * when a required @RequestHeader is absent. For Authorization headers this
     * must return HTTP 401, not HTTP 500. The static message is safe to expose.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("Authorization".equalsIgnoreCase(ex.getHeaderName())) {
            return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Required request header is missing");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
