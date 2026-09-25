package com.example.appointmentservice.exception;

/**
 * Thrown when an incoming request is missing or has a malformed Authorization header.
 * Handled by {@link GlobalExceptionHandler} as HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
