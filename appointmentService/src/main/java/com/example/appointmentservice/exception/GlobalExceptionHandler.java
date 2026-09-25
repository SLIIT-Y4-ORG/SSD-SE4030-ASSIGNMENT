package com.example.appointmentservice.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.appointmentservice.exception.UnauthorizedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // V-03 fix: logger for server-side recording of unexpected exceptions.
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        body.put("message", "Validation failed");
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * V-03 fix (CWE-209): Downstream microservice unavailable.
     * Returns HTTP 503 with only the static safe message stored in the exception.
     * The message is always set to a literal string by the service layer — never
     * derived from ex.getMessage() of the underlying cause.
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<?> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Downstream service unavailable", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /**
     * Token-propagation fix: missing, blank, or malformed Authorization header.
     * Returns HTTP 401 with a static safe message.
     * The header value is never logged.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized request to appointment service: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    /**
     * V-03 fix (CWE-209): Do not return ex.getMessage() to the client.
     * Internal details such as hostnames, ports and database constraint names
     * contained in exception messages must not be disclosed over HTTP.
     * The full exception is logged server-side for operator visibility.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex) {
        log.error("Unhandled exception in appointment service", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
