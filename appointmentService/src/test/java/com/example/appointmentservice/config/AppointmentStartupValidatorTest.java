package com.example.appointmentservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V-04 -- Hard-coded internal payment API key (CWE-798)
 *
 * Verifies that missing or blank configuration for the PAYMENT_INTERNAL_API_KEY
 * causes the application to fail fast on startup via the AppointmentStartupValidator.
 */
@DisplayName("V-04: AppointmentStartupValidator tests")
class AppointmentStartupValidatorTest {

    private final AppointmentStartupValidator validator = new AppointmentStartupValidator();
    private final ApplicationArguments args = new DefaultApplicationArguments(new String[]{});

    @Test
    @DisplayName("Startup fails if internal API key is null")
    void startupFailsIfKeyIsNull() {
        ReflectionTestUtils.setField(validator, "paymentInternalApiKey", null);
        
        assertThatThrownBy(() -> validator.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAYMENT_INTERNAL_API_KEY environment variable is required");
    }

    @Test
    @DisplayName("Startup fails if internal API key is blank")
    void startupFailsIfKeyIsBlank() {
        ReflectionTestUtils.setField(validator, "paymentInternalApiKey", "   ");
        
        assertThatThrownBy(() -> validator.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAYMENT_INTERNAL_API_KEY environment variable is required");
    }

    @Test
    @DisplayName("Startup succeeds if internal API key is valid")
    void startupSucceedsIfKeyIsValid() {
        ReflectionTestUtils.setField(validator, "paymentInternalApiKey", "test-secret-key-12345");
        
        // Should not throw any exception
        validator.run(args);
    }
}
