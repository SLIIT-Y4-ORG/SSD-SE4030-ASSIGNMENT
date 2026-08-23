package com.example.doctorservice.util;

import com.example.doctorservice.client.UserServiceClient;
import com.example.doctorservice.dto.TokenValidationResponse;
import com.example.doctorservice.exception.ForbiddenException;
import com.example.doctorservice.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthHelperTest {
    private final UserServiceClient users = mock(UserServiceClient.class);
    private final AuthHelper auth = new AuthHelper(users);

    @Test
    void authenticatedRoleIsValidatedOnceAndReturned() {
        TokenValidationResponse patient = TokenValidationResponse.builder()
                .valid(true).userId(UUID.randomUUID()).role("PATIENT").build();
        when(users.validateToken("valid-token")).thenReturn(patient);

        assertSame(patient, auth.requireRole("Bearer valid-token", "PATIENT"));
        verify(users, times(1)).validateToken("valid-token");
    }

    @Test
    void wrongRoleIsForbiddenAndInvalidTokenIsUnauthorized() {
        when(users.validateToken("patient-token")).thenReturn(TokenValidationResponse.builder()
                .valid(true).userId(UUID.randomUUID()).role("PATIENT").build());
        when(users.validateToken("invalid-token")).thenReturn(TokenValidationResponse.builder().valid(false).build());

        assertThrows(ForbiddenException.class,
                () -> auth.requireRole("Bearer patient-token", "ADMIN"));
        assertThrows(UnauthorizedException.class,
                () -> auth.requireRole("Bearer invalid-token", "PATIENT"));
        assertThrows(UnauthorizedException.class,
                () -> auth.requireAuth(null));
    }
}
