package com.example.userservice.security;

import com.example.userservice.exception.AuthRegistrationException;
import com.example.userservice.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    void shortLegacyPasswordsCanBeMigratedButCannotBeCreatedAsNewPasswords() {
        PasswordService service = new PasswordService();

        String migrated = service.hashLegacyPassword("old-pass");

        assertTrue(service.matches("old-pass", migrated));
        assertThrows(IllegalArgumentException.class, () -> service.hash("old-pass"));
    }

    @Test
    void passwordAndTokenVersionsAreNeverSerialized() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).password("secret-value")
                .authVersion(7L).refreshVersion(9L).build();

        String json = new ObjectMapper().writeValueAsString(user);

        assertFalse(json.contains("secret-value"));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("authVersion"));
        assertFalse(json.contains("refreshVersion"));
    }

    @Test
    void signedTokensRejectTamperingAndWrongTokenType() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC);
        TokenService service = new TokenService(SECRET, clock);
        UUID userId = UUID.randomUUID();
        String accessToken = service.issue(userId, TokenService.TokenType.ACCESS, 3);

        TokenService.TokenClaims claims = service.verify(accessToken, TokenService.TokenType.ACCESS);
        assertEquals(userId, claims.userId());
        assertEquals(3, claims.version());
        assertThrows(AuthRegistrationException.class,
                () -> service.verify(accessToken + "tampered", TokenService.TokenType.ACCESS));
        assertThrows(AuthRegistrationException.class,
                () -> service.verify(accessToken, TokenService.TokenType.REFRESH));
    }

    @Test
    void expiredAndMalformedTokensAreRejected() {
        Clock issuedAt = Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC);
        Clock afterExpiry = Clock.fixed(Instant.parse("2026-08-16T01:00:01Z"), ZoneOffset.UTC);
        UUID userId = UUID.randomUUID();
        String token = new TokenService(SECRET, issuedAt)
                .issue(userId, TokenService.TokenType.ACCESS, 0);

        TokenService verifier = new TokenService(SECRET, afterExpiry);
        assertThrows(AuthRegistrationException.class,
                () -> verifier.verify(token, TokenService.TokenType.ACCESS));
        assertThrows(AuthRegistrationException.class,
                () -> verifier.verify("not-a-token", TokenService.TokenType.ACCESS));
        assertThrows(AuthRegistrationException.class,
                () -> verifier.verify("x".repeat(2049), TokenService.TokenType.ACCESS));
    }
}
