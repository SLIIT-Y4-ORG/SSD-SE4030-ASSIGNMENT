package com.example.doctorservice.exception;

import java.util.UUID;

/**
 * Thrown when a slot is already RESERVED and a second reserve attempt is made.
 * Also thrown when an optimistic locking conflict is detected during concurrent reservation.
 */
public class SlotNotAvailableException extends RuntimeException {
    public SlotNotAvailableException(UUID slotId) {
        super("Slot is not available for reservation: " + slotId);
    }
    public SlotNotAvailableException(String message) {
        super(message);
    }
}
