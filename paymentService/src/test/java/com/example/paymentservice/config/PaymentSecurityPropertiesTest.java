package com.example.paymentservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V-04 -- Hard-coded internal payment API key (CWE-798)
 *
 * Verifies that missing or blank configuration for the PAYMENT_INTERNAL_API_KEY
 * causes the application configuration to fail fast when internal authentication
 * is enabled.
 */
@DisplayName("V-04: PaymentSecurityProperties tests")
class PaymentSecurityPropertiesTest {

    @Test
    @DisplayName("Validation fails if auth enabled but key is null")
    void failsIfAuthEnabledAndKeyNull() {
        assertThatThrownBy(() -> new PaymentSecurityProperties(null, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAYMENT_INTERNAL_API_KEY environment variable is required");
    }

    @Test
    @DisplayName("Validation fails if auth enabled but key is blank")
    void failsIfAuthEnabledAndKeyBlank() {
        assertThatThrownBy(() -> new PaymentSecurityProperties("   ", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAYMENT_INTERNAL_API_KEY environment variable is required");
    }

    @Test
    @DisplayName("Validation succeeds if auth enabled and key is valid")
    void succeedsIfAuthEnabledAndKeyValid() {
        PaymentSecurityProperties props = new PaymentSecurityProperties("test-secret", true);
        assertThat(props.internalApiKey()).isEqualTo("test-secret");
        assertThat(props.internalAuthEnabled()).isTrue();
    }

    @Test
    @DisplayName("Validation succeeds if auth disabled even if key is null")
    void succeedsIfAuthDisabledAndKeyNull() {
        PaymentSecurityProperties props = new PaymentSecurityProperties(null, false);
        assertThat(props.internalApiKey()).isNull();
        assertThat(props.internalAuthEnabled()).isFalse();
    }
}
