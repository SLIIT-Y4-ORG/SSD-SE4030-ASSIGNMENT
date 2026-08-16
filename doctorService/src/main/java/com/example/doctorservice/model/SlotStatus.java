package com.example.doctorservice.model;

/**
 * Lifecycle of a doctor's time slot.
 *
 * AVAILABLE → RESERVED  : via POST /api/slots/{id}/reserve   (appointment-service)
 * RESERVED  → AVAILABLE : via POST /api/slots/{id}/release   (saga compensation)
 */
public enum SlotStatus {
    AVAILABLE,
    RESERVED
}
