package com.example.userservice.service;

import com.example.userservice.dto.TokenValidationResponse;
import com.example.userservice.model.*;
import com.example.userservice.exception.AuthRegistrationException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.dto.GoogleUserInfo;
import com.example.userservice.security.GoogleOAuthService;
import com.example.userservice.security.PasswordService;
import com.example.userservice.security.TokenService;
import com.example.userservice.security.TokenService.TokenType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final GoogleOAuthService googleOAuthService;

    public AuthServiceImpl(UserRepository userRepository, PasswordService passwordService, TokenService tokenService) {
        this(userRepository, passwordService, tokenService, null);
    }

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordService passwordService, TokenService tokenService, GoogleOAuthService googleOAuthService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.googleOAuthService = googleOAuthService;
    }

    @Override
    public User registerUser(User user) {
        String email = normalizeEmail(user.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthRegistrationException("User with email already exists");
        }
        
        // Public registration must never accept identity or privilege fields from the client.
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(UserRole.PATIENT);
        user.setEnabled(true);
        user.setAuthVersion(0L);
        user.setRefreshVersion(0L);
        user.setPassword(passwordService.hash(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public AuthResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail())).orElse(null);
        if (user == null) {
            passwordService.performDummyVerification(request.getPassword());
            throw new AuthRegistrationException("Invalid credentials");
        }
        if (!passwordService.matches(request.getPassword(), user.getPassword()) || !user.isEnabled()) {
            throw new AuthRegistrationException("Invalid credentials");
        }

        if (passwordService.needsUpgrade(user.getPassword())) {
            user.setPassword(passwordService.hashLegacyPassword(request.getPassword()));
        }

        long refreshVersion = refreshVersion(user) + 1;
        user.setRefreshVersion(refreshVersion);
        userRepository.save(user);

        String token = tokenService.issue(user.getId(), TokenType.ACCESS, authVersion(user));
        String refreshToken = tokenService.issue(user.getId(), TokenType.REFRESH, refreshVersion);
        
        AuthResponse response = new AuthResponse();
        response.setAccessToken(token);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        response.setUser(user);
        
        return response;
    }

    @Override
    @Transactional
    public void logoutUser(String accessToken) {
        TokenService.TokenClaims claims = tokenService.verify(accessToken, TokenType.ACCESS);
        User user = requireEnabledUserForUpdate(claims.userId());
        requireVersion(claims.version(), authVersion(user));
        user.setAuthVersion(authVersion(user) + 1);
        user.setRefreshVersion(refreshVersion(user) + 1);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        TokenService.TokenClaims claims = tokenService.verify(refreshToken, TokenType.REFRESH);
        User user = requireEnabledUserForUpdate(claims.userId());
        requireVersion(claims.version(), refreshVersion(user));
        long nextRefreshVersion = refreshVersion(user) + 1;
        user.setRefreshVersion(nextRefreshVersion);
        userRepository.save(user);
        AuthResponse response = new AuthResponse();
        response.setAccessToken(tokenService.issue(user.getId(), TokenType.ACCESS, authVersion(user)));
        response.setRefreshToken(tokenService.issue(user.getId(), TokenType.REFRESH, nextRefreshVersion));
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        response.setUser(user);
        return response;
    }

    @Override
    public User getCurrentUser(String accessToken) {
        TokenService.TokenClaims claims = tokenService.verify(accessToken, TokenType.ACCESS);
        User user = requireEnabledUser(claims.userId());
        requireVersion(claims.version(), authVersion(user));
        return user;
    }

    @Override
    public TokenValidationResponse validateToken(String accessToken) {
        try {
            User user = getCurrentUser(accessToken);
            
            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("Invalid token")
                    .build();
        }
    }

    @Override
    public String getGoogleAuthorizationUrl(String state, String redirectUri) {
        return googleOAuthService.buildAuthorizationUrl(state, redirectUri);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String code, String redirectUri) {
        GoogleUserInfo googleUser = googleOAuthService.exchangeCodeForUserInfo(code, redirectUri);
        String email = normalizeEmail(googleUser.getEmail());

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            String displayName = googleUser.getName();
            if (displayName == null || displayName.isBlank()) {
                displayName = email.split("@")[0];
            }
            // Generate a secure, unguessable random password hash so password-based login cannot be brute-forced
            String randomSecret = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();

            user = User.builder()
                    .id(UUID.randomUUID())
                    .email(email)
                    .name(displayName.trim())
                    .role(UserRole.PATIENT)
                    .enabled(true)
                    .authVersion(0L)
                    .refreshVersion(0L)
                    .password(passwordService.hash(randomSecret))
                    .build();
            user = userRepository.save(user);
        } else {
            if (!user.isEnabled()) {
                throw new AuthRegistrationException("User account is disabled");
            }
            if ((user.getName() == null || user.getName().isBlank()) && googleUser.getName() != null) {
                user.setName(googleUser.getName().trim());
                userRepository.save(user);
            }
        }

        long refreshVersion = refreshVersion(user) + 1;
        user.setRefreshVersion(refreshVersion);
        userRepository.save(user);

        String accessToken = tokenService.issue(user.getId(), TokenType.ACCESS, authVersion(user));
        String refreshToken = tokenService.issue(user.getId(), TokenType.REFRESH, refreshVersion);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        response.setUser(user);

        return response;
    }

    private User requireEnabledUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthRegistrationException("Invalid or expired token"));
        if (!user.isEnabled()) {
            throw new AuthRegistrationException("Invalid or expired token");
        }
        return user;
    }

    private User requireEnabledUserForUpdate(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthRegistrationException("Invalid or expired token"));
        if (!user.isEnabled()) {
            throw new AuthRegistrationException("Invalid or expired token");
        }
        return user;
    }

    private void requireVersion(long tokenVersion, long currentVersion) {
        if (tokenVersion != currentVersion) {
            throw new AuthRegistrationException("Invalid or expired token");
        }
    }

    private long authVersion(User user) {
        return user.getAuthVersion() == null ? 0L : user.getAuthVersion();
    }

    private long refreshVersion(User user) {
        return user.getRefreshVersion() == null ? 0L : user.getRefreshVersion();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
