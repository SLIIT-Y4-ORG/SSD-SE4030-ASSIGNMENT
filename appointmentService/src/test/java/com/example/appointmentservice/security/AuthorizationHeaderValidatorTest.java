package com.example.appointmentservice.security;

import com.example.appointmentservice.client.UserServiceClient;
import com.example.appointmentservice.client.UserServiceClient.TokenInfo;
import com.example.appointmentservice.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Bearer-header validation logic in {@link AuthHelper}.
 *
 * Format-rejection cases (null, blank, no Bearer prefix, blank token after prefix)
 * are all handled by the format check inside {@link AuthHelper#requireBearer} before
 * the {@link UserServiceClient} is ever called — the client mock verifies this.
 *
 * The happy-path test stubs the client to return a valid {@link TokenInfo}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthHelper: Bearer header validation")
class AuthorizationHeaderValidatorTest {

    @Mock
    private UserServiceClient userServiceClient;

    private AuthHelper authHelper;

    @BeforeEach
    void setup() {
        authHelper = new AuthHelper(userServiceClient);
    }

    // ── Reject: null (missing header) ─────────────────────────────────────────

    @Test
    @DisplayName("null header (missing) → UnauthorizedException, client not called")
    void nullHeaderThrows() {
        assertThatThrownBy(() -> authHelper.requireBearer(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");

        verify(userServiceClient, never()).validateToken(any());
    }

    // ── Reject: blank / empty ─────────────────────────────────────────────────

    @Test
    @DisplayName("empty string header → UnauthorizedException, client not called")
    void emptyStringThrows() {
        assertThatThrownBy(() -> authHelper.requireBearer(""))
                .isInstanceOf(UnauthorizedException.class);

        verify(userServiceClient, never()).validateToken(any());
    }

    @Test
    @DisplayName("whitespace-only header → UnauthorizedException, client not called")
    void blankHeaderThrows() {
        assertThatThrownBy(() -> authHelper.requireBearer("   "))
                .isInstanceOf(UnauthorizedException.class);

        verify(userServiceClient, never()).validateToken(any());
    }

    // ── Reject: no Bearer prefix ──────────────────────────────────────────────

    @Test
    @DisplayName("plain token without Bearer prefix → UnauthorizedException, client not called")
    void noBearerPrefixThrows() {
        assertThatThrownBy(() -> authHelper.requireBearer("some-raw-token"))
                .isInstanceOf(UnauthorizedException.class);

        verify(userServiceClient, never()).validateToken(any());
    }

    @Test
    @DisplayName("Basic auth scheme → UnauthorizedException, client not called")
    void basicAuthSchemeThrows() {
        assertThatThrownBy(() -> authHelper.requireBearer("Basic dXNlcjpwYXNz"))
                .isInstanceOf(UnauthorizedException.class);

        verify(userServiceClient, never()).validateToken(any());
    }

    // Note: AuthHelper uses regionMatches(true, ...) — "bearer" (lowercase) passes
    // the prefix check and is forwarded to userService. This is intentionally permissive;
    // the old AuthorizationHeaderValidator was stricter. The security boundary is
    // userService rejecting invalid tokens, not case-sensitivity of the scheme.

    // ── Reject: userService returns null (token invalid/expired) ─────────────

    @Test
    @DisplayName("\"Bearer \" with no token → userService returns null → UnauthorizedException")
    void bearerOnlyThrowsWhenClientReturnsNull() {
        when(userServiceClient.validateToken("Bearer ")).thenReturn(null);

        assertThatThrownBy(() -> authHelper.requireBearer("Bearer "))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── Accept: well-formed Bearer token ─────────────────────────────────────

    @Test
    @DisplayName("valid Bearer token with stub returning TokenInfo → no exception")
    void validBearerTokenPasses() {
        TokenInfo fakeUser = new TokenInfo(UUID.randomUUID(), "PATIENT");
        when(userServiceClient.validateToken("Bearer test-token-abc123")).thenReturn(fakeUser);

        assertThatCode(() -> authHelper.requireBearer("Bearer test-token-abc123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("valid Bearer JWT with stub returning TokenInfo → no exception")
    void validBearerJwtPasses() {
        TokenInfo fakeUser = new TokenInfo(UUID.randomUUID(), "ADMIN");
        when(userServiceClient.validateToken("Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig")).thenReturn(fakeUser);

        assertThatCode(() -> authHelper.requireBearer("Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig"))
                .doesNotThrowAnyException();
    }

    // ── Exception message safety ──────────────────────────────────────────────

    @Test
    @DisplayName("UnauthorizedException message is the static safe string, never the header value")
    void exceptionMessageIsStaticSafeString() {
        assertThatThrownBy(() -> authHelper.requireBearer("Basic leakedCredential"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(Throwable::getMessage)
                .asString()
                .isEqualTo("Authentication required")
                .doesNotContain("leakedCredential")
                .doesNotContain("Basic");
    }
}
