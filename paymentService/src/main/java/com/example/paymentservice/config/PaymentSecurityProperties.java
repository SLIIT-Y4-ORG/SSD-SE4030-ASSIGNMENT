package com.example.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.security")
public record PaymentSecurityProperties(String internalApiKey, boolean internalAuthEnabled) {
    public PaymentSecurityProperties {
        if (internalAuthEnabled && (internalApiKey == null || internalApiKey.isBlank())) {
            throw new IllegalStateException(
                    "PAYMENT_INTERNAL_API_KEY environment variable is required and must not be blank when internal auth is enabled");
        }
    }
}
