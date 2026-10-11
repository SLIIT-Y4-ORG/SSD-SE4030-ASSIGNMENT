package com.example.paymentservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.example.paymentservice.config.PaymentSecurityProperties;
import com.example.paymentservice.exception.UnauthorizedException;

class InternalAuthServiceTest {

    @Test
    void rejectsMissingAndIncorrectKeysEvenIfLegacyToggleIsFalse() {
        InternalAuthService auth = new InternalAuthService(
                new PaymentSecurityProperties("correct-secret", false));

        assertThrows(UnauthorizedException.class, () -> auth.verifyInternalApiKey(null));
        assertThrows(UnauthorizedException.class, () -> auth.verifyInternalApiKey("wrong-secret"));
        assertDoesNotThrow(() -> auth.verifyInternalApiKey("correct-secret"));
    }
}
