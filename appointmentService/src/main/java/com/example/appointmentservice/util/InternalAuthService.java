package com.example.appointmentservice.util;

import com.example.appointmentservice.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalAuthService {
    private final String configuredKey;

    public InternalAuthService(@Value("${services.payment.internal-api-key}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public void verify(String providedKey) {
        if (configuredKey == null || configuredKey.isBlank() || providedKey == null || providedKey.isBlank()
                || !MessageDigest.isEqual(configuredKey.getBytes(StandardCharsets.UTF_8),
                        providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Invalid internal API key");
        }
    }
}
