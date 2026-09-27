package com.example.appointmentservice.exception;

/**
 * V-03 fix (CWE-209): Exception for safe client-facing service-unavailability errors.
 *
 * Only safe, statically defined messages must be passed to this exception.
 * Never construct the message from ex.getMessage(), downstream response bodies,
 * URLs, hostnames, ports, or any other runtime data.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String safeMessage) {
        super(safeMessage);
    }
}
