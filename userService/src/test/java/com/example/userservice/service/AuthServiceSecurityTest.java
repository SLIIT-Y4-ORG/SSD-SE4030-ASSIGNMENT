package com.example.userservice.service;

import com.example.userservice.model.User;
import com.example.userservice.model.UserRole;
import com.example.userservice.model.AuthResponse;
import com.example.userservice.model.LoginRequest;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.PasswordService;
import com.example.userservice.security.TokenService;
import com.example.userservice.exception.AuthRegistrationException;
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

    @Test
    void logoutRevokesAccessAndRefreshTokens() {
        UserRepository repository = mock(UserRepository.class);
        PasswordService passwords = new PasswordService();
        TokenService tokens = new TokenService("test-secret-that-is-at-least-32-characters-long");
        AuthServiceImpl service = new AuthServiceImpl(repository, passwords, tokens);
        User user = User.builder().id(java.util.UUID.randomUUID()).enabled(true)
                .authVersion(0L).refreshVersion(0L).build();
        when(repository.findById(user.getId())).thenReturn(java.util.Optional.of(user));
        when(repository.findByIdForUpdate(user.getId())).thenReturn(java.util.Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String access = tokens.issue(user.getId(), TokenService.TokenType.ACCESS, 0);
        String refresh = tokens.issue(user.getId(), TokenService.TokenType.REFRESH, 0);

        service.logoutUser(access);

        assertThrows(AuthRegistrationException.class, () -> service.getCurrentUser(access));
        assertThrows(AuthRegistrationException.class, () -> service.refreshToken(refresh));
        assertEquals(1L, user.getAuthVersion());
        assertEquals(1L, user.getRefreshVersion());
    }

    @Test
    void refreshTokensAreRotatedAndCannotBeReplayed() {
        UserRepository repository = mock(UserRepository.class);
        PasswordService passwords = new PasswordService();
        TokenService tokens = new TokenService("test-secret-that-is-at-least-32-characters-long");
        AuthServiceImpl service = new AuthServiceImpl(repository, passwords, tokens);
        User user = User.builder().id(java.util.UUID.randomUUID()).enabled(true)
                .authVersion(0L).refreshVersion(0L).build();
        when(repository.findById(user.getId())).thenReturn(java.util.Optional.of(user));
        when(repository.findByIdForUpdate(user.getId())).thenReturn(java.util.Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String refresh = tokens.issue(user.getId(), TokenService.TokenType.REFRESH, 0);

        AuthResponse rotated = service.refreshToken(refresh);

        assertNotNull(rotated.getAccessToken());
        assertNotNull(rotated.getRefreshToken());
        assertThrows(AuthRegistrationException.class, () -> service.refreshToken(refresh));
    }

    @Test
    void disabledAccountsCannotLogin() {
        UserRepository repository = mock(UserRepository.class);
        PasswordService passwords = new PasswordService();
        TokenService tokens = new TokenService("test-secret-that-is-at-least-32-characters-long");
        AuthServiceImpl service = new AuthServiceImpl(repository, passwords, tokens);
        User user = User.builder().id(java.util.UUID.randomUUID()).email("disabled@example.com")
                .password(passwords.hash("StrongPassword!1")).enabled(false).build();
        when(repository.findByEmailIgnoreCase("disabled@example.com")).thenReturn(java.util.Optional.of(user));

        assertThrows(AuthRegistrationException.class,
                () -> service.loginUser(new LoginRequest("disabled@example.com", "StrongPassword!1")));
        verify(repository, never()).save(any());
    }

    @Test
    void unknownAndIncorrectCredentialsReturnTheSameError() {
        UserRepository repository = mock(UserRepository.class);
        PasswordService passwords = new PasswordService();
        TokenService tokens = new TokenService("test-secret-that-is-at-least-32-characters-long");
        AuthServiceImpl service = new AuthServiceImpl(repository, passwords, tokens);
        User user = User.builder().id(java.util.UUID.randomUUID()).email("known@example.com")
                .password(passwords.hash("StrongPassword!1")).enabled(true).build();
        when(repository.findByEmailIgnoreCase("known@example.com")).thenReturn(java.util.Optional.of(user));
        when(repository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(java.util.Optional.empty());

        AuthRegistrationException unknown = assertThrows(AuthRegistrationException.class,
                () -> service.loginUser(new LoginRequest("unknown@example.com", "StrongPassword!1")));
        AuthRegistrationException incorrect = assertThrows(AuthRegistrationException.class,
                () -> service.loginUser(new LoginRequest("known@example.com", "WrongPassword!1")));

        assertEquals(unknown.getMessage(), incorrect.getMessage());
        assertEquals("Invalid credentials", unknown.getMessage());
    }
}
