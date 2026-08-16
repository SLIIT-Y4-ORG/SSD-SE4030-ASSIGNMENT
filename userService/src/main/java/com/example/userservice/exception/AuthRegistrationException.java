package com.example.userservice.exception;

public class AuthRegistrationException extends RuntimeException {
    public AuthRegistrationException(String message) {
        super(message);
    }

    public AuthRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
