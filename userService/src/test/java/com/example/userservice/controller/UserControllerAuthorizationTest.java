package com.example.userservice.controller;

import com.example.userservice.exception.ForbiddenException;
import com.example.userservice.model.User;
import com.example.userservice.model.UserRole;
import com.example.userservice.service.AuthService;
import com.example.userservice.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
        when(users.updateUser(callerId, update, false)).thenReturn(caller);

        controller.updateUser("Bearer valid-token", callerId, update);

        verify(users).updateUser(callerId, update, false);
    }
}
