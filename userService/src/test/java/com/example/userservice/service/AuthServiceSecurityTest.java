package com.example.userservice.service;

import com.example.userservice.model.User;
import com.example.userservice.model.UserRole;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.PasswordService;
import com.example.userservice.security.TokenService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceSecurityTest {
    @Test
    void publicRegistrationCannotChooseAnAdministratorRole() {
        UserRepository repository = mock(UserRepository.class);
        PasswordService passwords = new PasswordService();
        TokenService tokens = new TokenService("test-secret-that-is-at-least-32-characters-long");
        AuthServiceImpl service = new AuthServiceImpl(repository, passwords, tokens);
        User request = User.builder().name("Attacker").email("attacker@example.com")
                .password("StrongPassword!1").role(UserRole.ADMIN).build();
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.registerUser(request);

        assertEquals(UserRole.PATIENT, saved.getRole());
        assertTrue(passwords.isHash(saved.getPassword()));
        verify(repository).save(saved);
    }
}
