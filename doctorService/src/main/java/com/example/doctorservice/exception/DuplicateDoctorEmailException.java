package com.example.doctorservice.exception;

public class DuplicateDoctorEmailException extends RuntimeException {
    public DuplicateDoctorEmailException(String email) {
        super("A doctor with email already exists: " + email);
    }
}
