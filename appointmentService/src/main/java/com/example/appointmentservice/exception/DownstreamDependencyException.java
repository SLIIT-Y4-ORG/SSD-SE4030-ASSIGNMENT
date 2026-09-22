package com.example.appointmentservice.exception;

/**
 * V-03 fix (CWE-209): Typed exception for downstream microservice communication failures.
 *
 * Used exclusively by service clients (PatientServiceClient, DoctorServiceClient, etc.)
 * to signal that a downstream dependency could not be reached or returned an unexpected
 * 5xx error. The original cause is always preserved for server-side logging.
 *
 * The message passed to this constructor MUST be a static, safe string that cannot
 * contain any downstream response body, URL, hostname, port, or exception detail.
 */
public class DownstreamDependencyException extends RuntimeException {

    public DownstreamDependencyException(String safeMessage, Throwable cause) {
        super(safeMessage, cause);
    }
}
