package com.example.doctorservice.exception;

import java.util.UUID;

public class SlotNotFoundException extends RuntimeException {
    public SlotNotFoundException(UUID id) {
        super("Slot not found with ID: " + id);
    }
}
