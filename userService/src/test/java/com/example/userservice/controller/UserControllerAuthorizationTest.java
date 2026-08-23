package com.example.userservice.controller;

import com.example.userservice.exception.ForbiddenException;
import com.example.userservice.exception.AuthRegistrationException;
import com.example.userservice.model.User;
import com.example.userservice.model.UserRole;
import com.example.userservice.service.AuthService;
import com.example.userservice.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserControllerAuthorizationTest {
    private final UserService users = mock(UserService.class);
    private final AuthService auth = mock(AuthService.class);
    private final UserController controller = new UserController(users, auth);

    @Test
    void patientCannotReadAnotherUsersRecord() {
        UUID callerId = UUID.randomUUID();
        when(auth.getCurrentUser("valid-token")).thenReturn(
                User.builder().id(callerId).role(UserRole.PATIENT).email("patient@example.com").build());

        assertThrows(ForbiddenException.class,
                () -> controller.getUserById("Bearer valid-token", UUID.randomUUID()));
        verifyNoInteractions(users);
    }

    @Test
    void selfUpdateCannotChangeRole() {
        UUID callerId = UUID.randomUUID();
        User caller = User.builder().id(callerId).role(UserRole.PATIENT).email("patient@example.com").build();
        User update = User.builder().email("patient@example.com").role(UserRole.ADMIN).build();
        when(auth.getCurrentUser("valid-token")).thenReturn(caller);
        when(users.updateUser(callerId, update)).thenReturn(caller);

        controller.updateUser("Bearer valid-token", callerId, update);

        verify(users).updateUser(callerId, update);
    }

    @Test
    void missingBearerTokenIsUnauthorized() {
        assertThrows(AuthRegistrationException.class,
                () -> controller.getUserById(null, UUID.randomUUID()));
        verifyNoInteractions(users, auth);
    }

    @Test
    void patientCannotListUsers() {
        when(auth.getCurrentUser("valid-token")).thenReturn(
                User.builder().id(UUID.randomUUID()).role(UserRole.PATIENT).build());

        assertThrows(ForbiddenException.class, () -> controller.getAllUsers("Bearer valid-token"));
        verify(users, never()).getAllUsers();
    }

    @Test
    void receptionistCanListOnlyPatientAccounts() {
        when(auth.getCurrentUser("valid-token")).thenReturn(
                User.builder().id(UUID.randomUUID()).role(UserRole.RECEPTIONIST).build());
        when(users.getPatientUsers()).thenReturn(List.of());

        controller.getPatientUsers("Bearer valid-token");

        verify(users).getPatientUsers();
        assertThrows(ForbiddenException.class, () -> controller.getAllUsers("Bearer valid-token"));
        verify(users, never()).getAllUsers();
    }

    @Test
    void patientCannotListPatientAccounts() {
        when(auth.getCurrentUser("valid-token")).thenReturn(
                User.builder().id(UUID.randomUUID()).role(UserRole.PATIENT).build());

        assertThrows(ForbiddenException.class,
                () -> controller.getPatientUsers("Bearer valid-token"));
        verify(users, never()).getPatientUsers();
    }
}
