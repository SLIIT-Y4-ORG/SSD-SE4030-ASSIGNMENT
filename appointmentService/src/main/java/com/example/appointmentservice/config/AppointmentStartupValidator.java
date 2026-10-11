package com.example.appointmentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * V-04 -- Hard-coded internal payment API key (CWE-798)
 *
 * Ensures that the application fails to start if the internal API key for
 * authenticating with the Payment Service is missing or blank, preventing
 * the application from starting in an insecure or broken state now that the
 * hard-coded fallback has been removed.
 */
@Component
public class AppointmentStartupValidator implements ApplicationRunner {

    @Value("${services.payment.internal-api-key:}")
    private String paymentInternalApiKey;

    @Override
    public void run(ApplicationArguments args) {
        if (paymentInternalApiKey == null || paymentInternalApiKey.isBlank()) {
            throw new IllegalStateException(
                "PAYMENT_INTERNAL_API_KEY environment variable is required and must not be blank"
            );
        }
    }
}
