package com.example.userservice.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("User already exists with email: " + email);
    }
}
