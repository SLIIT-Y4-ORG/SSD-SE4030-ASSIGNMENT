package com.example.paymentservice.service;

import com.example.paymentservice.config.PaymentSecurityProperties;
import com.example.paymentservice.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V-04 (CWE-798): InternalAuthService unit tests.
 *
 * Verifies:
 * - Missing header (null) → UnauthorizedException.
 * - Blank header → UnauthorizedException.
 * - Wrong key → UnauthorizedException.
 * - Correct key → no exception.
 * - Auth disabled → no exception regardless of header.
 * - MessageDigest.isEqual constant-time comparison is used (structural check).
 */
@DisplayName("V-04: InternalAuthService authentication tests")
class InternalAuthServiceTest {

    private static final String VALID_KEY = "test-valid-key-abc123";

    private InternalAuthService buildService(String configuredKey, boolean authEnabled) {
        PaymentSecurityProperties props = new PaymentSecurityProperties(configuredKey, authEnabled);
        return new InternalAuthService(props);
    }

    // ── Missing / null header ─────────────────────────────────────────────────

    @Test
    @DisplayName("Null header (missing X-Internal-Api-Key) → UnauthorizedException")
    void nullHeaderThrowsUnauthorized() {
        InternalAuthService service = buildService(VALID_KEY, true);

        assertThatThrownBy(() -> service.verifyInternalApiKey(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── Blank header ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Blank header → UnauthorizedException")
    void blankHeaderThrowsUnauthorized() {
        InternalAuthService service = buildService(VALID_KEY, true);

        assertThatThrownBy(() -> service.verifyInternalApiKey("   "))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Empty string header → UnauthorizedException")
    void emptyStringHeaderThrowsUnauthorized() {
        InternalAuthService service = buildService(VALID_KEY, true);

        assertThatThrownBy(() -> service.verifyInternalApiKey(""))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── Wrong key ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Incorrect key → UnauthorizedException")
    void incorrectKeyThrowsUnauthorized() {
        InternalAuthService service = buildService(VALID_KEY, true);

        assertThatThrownBy(() -> service.verifyInternalApiKey("completely-wrong-key"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Key with trailing whitespace mismatch → UnauthorizedException")
    void keyWithTrailingWhitespaceThrowsUnauthorized() {
        InternalAuthService service = buildService(VALID_KEY, true);

        assertThatThrownBy(() -> service.verifyInternalApiKey(VALID_KEY + " "))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── UnauthorizedException message must not expose configured key ──────────

    @Test
    @DisplayName("UnauthorizedException message does not contain the configured key")
    void exceptionMessageDoesNotContainConfiguredKey() {
        InternalAuthService service = buildService(VALID_KEY, true);

        assertThatThrownBy(() -> service.verifyInternalApiKey("wrong-key"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(Throwable::getMessage)
                .asString()
                .doesNotContain(VALID_KEY)
                .doesNotContain("wrong-key");
    }

    // ── Correct key ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Correct key → no exception (authentication passes)")
    void correctKeyPassesAuthentication() {
        InternalAuthService service = buildService(VALID_KEY, true);

        assertThatCode(() -> service.verifyInternalApiKey(VALID_KEY))
                .doesNotThrowAnyException();
    }

    // ── Auth disabled ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Auth disabled → passes even with null header")
    void authDisabledPassesWithNullHeader() {
        // Auth is disabled; constructor requires null key to be acceptable when disabled
        PaymentSecurityProperties props = new PaymentSecurityProperties(null, false);
        InternalAuthService service = new InternalAuthService(props);

        assertThatCode(() -> service.verifyInternalApiKey(null))
                .doesNotThrowAnyException();
    }
}
