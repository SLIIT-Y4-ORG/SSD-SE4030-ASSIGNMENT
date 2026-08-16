package com.example.appointmentservice.model;

public enum AppointmentStatus {
    PENDING,           // Awaiting payment
    CONFIRMED,         // Payment successful, appointment confirmed
    PAYMENT_FAILED,    // Payment failed
    COMPLETED,         // Appointment completed
    CANCELLED          // Appointment cancelled
}