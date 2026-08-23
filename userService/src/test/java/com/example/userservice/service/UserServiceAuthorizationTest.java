package com.example.userservice.service;

import com.example.userservice.exception.ForbiddenException;
import com.example.userservice.model.User;
import com.example.userservice.model.UserRole;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.PasswordService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceAuthorizationTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserService service = new UserService(repository, new PasswordService());

    @Test
    void profileUpdateCannotChangeRoleOrAccountState() {
        UUID id = UUID.randomUUID();
        User stored = User.builder().id(id).name("Patient").email("patient@example.com")
                .role(UserRole.PATIENT).enabled(true).build();
        User update = User.builder().name("Updated").email("updated@example.com")
                .role(UserRole.ADMIN).enabled(false).build();
        when(repository.findById(id)).thenReturn(Optional.of(stored));
        when(repository.save(stored)).thenReturn(stored);

        User result = service.updateUser(id, update);

        assertEquals(UserRole.PATIENT, result.getRole());
        assertTrue(result.isEnabled());
        assertEquals("updated@example.com", result.getEmail());
    }

    @Test
    void finalAdministratorCannotBeDeleted() {
        UUID id = UUID.randomUUID();
        User admin = User.builder().id(id).role(UserRole.ADMIN).build();
        when(repository.findById(id)).thenReturn(Optional.of(admin));
        when(repository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(ForbiddenException.class, () -> service.deleteUser(id));
        verify(repository, never()).deleteById(any());
    }
}
