package com.example.appointmentservice.security;

import com.example.appointmentservice.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AuthorizationHeaderValidator}.
 *
 * Verifies all rejection paths (null, blank, empty, missing Bearer prefix,
 * blank token) and the single happy path (well-formed Bearer token).
 * Uses only dummy token values.
 */
@DisplayName("AuthorizationHeaderValidator unit tests")
class AuthorizationHeaderValidatorTest {

    private final AuthorizationHeaderValidator validator = new AuthorizationHeaderValidator();

    // ── Reject: null (missing header) ─────────────────────────────────────────

    @Test
    @DisplayName("null header (missing) → UnauthorizedException")
    void nullHeaderThrows() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");
    }

    // ── Reject: blank / empty ─────────────────────────────────────────────────

    @Test
    @DisplayName("empty string header → UnauthorizedException")
    void emptyStringThrows() {
        assertThatThrownBy(() -> validator.validate(""))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("whitespace-only header → UnauthorizedException")
    void blankHeaderThrows() {
        assertThatThrownBy(() -> validator.validate("   "))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── Reject: no Bearer prefix ──────────────────────────────────────────────

    @Test
    @DisplayName("plain token without Bearer prefix → UnauthorizedException")
    void noBearerPrefixThrows() {
        assertThatThrownBy(() -> validator.validate("some-raw-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Basic auth scheme → UnauthorizedException")
    void basicAuthSchemeThrows() {
        assertThatThrownBy(() -> validator.validate("Basic dXNlcjpwYXNz"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("bearer (lowercase) is not accepted → UnauthorizedException")
    void lowercaseBearerThrows() {
        assertThatThrownBy(() -> validator.validate("bearer test-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── Reject: blank token after Bearer ─────────────────────────────────────

    @Test
    @DisplayName("\"Bearer \" with no token → UnauthorizedException")
    void bearerOnlyThrows() {
        assertThatThrownBy(() -> validator.validate("Bearer "))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("\"Bearer   \" with blank token → UnauthorizedException")
    void bearerBlankTokenThrows() {
        assertThatThrownBy(() -> validator.validate("Bearer   "))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── Accept: well-formed Bearer token ─────────────────────────────────────

    @Test
    @DisplayName("valid Bearer token → no exception")
    void validBearerTokenPasses() {
        assertThatCode(() -> validator.validate("Bearer test-token-abc123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("valid Bearer JWT → no exception")
    void validBearerJwtPasses() {
        assertThatCode(() -> validator.validate("Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig"))
                .doesNotThrowAnyException();
    }

    // ── Exception message safety ──────────────────────────────────────────────

    @Test
    @DisplayName("UnauthorizedException message is the static safe string, never the header value")
    void exceptionMessageIsStaticSafeString() {
        assertThatThrownBy(() -> validator.validate("Basic leakedCredential"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(Throwable::getMessage)
                .asString()
                .isEqualTo("Authentication required")
                .doesNotContain("leakedCredential")
                .doesNotContain("Basic");
    }
}
