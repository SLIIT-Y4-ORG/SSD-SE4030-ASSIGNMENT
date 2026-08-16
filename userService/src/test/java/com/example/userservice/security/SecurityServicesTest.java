package com.example.userservice.security;

import com.example.userservice.exception.AuthRegistrationException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityServicesTest {
    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

    @Test
    void passwordsAreSaltedAndVerifiedWithoutPlaintextStorage() {
        PasswordService service = new PasswordService();

        String first = service.hash("CorrectHorseBatteryStaple!");
        String second = service.hash("CorrectHorseBatteryStaple!");

        assertNotEquals(first, second, "a unique salt must be generated for every password");
        assertFalse(first.contains("CorrectHorseBatteryStaple!"));
        assertTrue(service.matches("CorrectHorseBatteryStaple!", first));
        assertFalse(service.matches("wrong-password", first));
    }

    @Test
    void signedTokensRejectTamperingAndWrongTokenType() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC);
        TokenService service = new TokenService(SECRET, clock);
        UUID userId = UUID.randomUUID();
        String accessToken = service.issue(userId, TokenService.TokenType.ACCESS);

        assertEquals(userId, service.verify(accessToken, TokenService.TokenType.ACCESS));
        assertThrows(AuthRegistrationException.class,
                () -> service.verify(accessToken + "tampered", TokenService.TokenType.ACCESS));
        assertThrows(AuthRegistrationException.class,
                () -> service.verify(accessToken, TokenService.TokenType.REFRESH));
    }
}
