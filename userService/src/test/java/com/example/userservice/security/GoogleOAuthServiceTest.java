package com.example.userservice.security;

import com.example.userservice.config.GoogleOAuthProperties;
import com.example.userservice.dto.GoogleUserInfo;
import com.example.userservice.exception.AuthRegistrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GoogleOAuthProperties properties;
    private GoogleOAuthService googleOAuthService;

    @BeforeEach
    void setUp() {
        properties = new GoogleOAuthProperties();
        properties.setClientId("test-client-id");
        properties.setClientSecret("test-client-secret");
        properties.setRedirectUri("http://localhost:5173/auth/callback");
        properties.setAuthorizationUri("https://accounts.google.com/o/oauth2/v2/auth");
        properties.setTokenUri("https://oauth2.googleapis.com/token");
        properties.setUserInfoUri("https://openidconnect.googleapis.com/v1/userinfo");

        googleOAuthService = new GoogleOAuthService(properties, restTemplate);
    }

    @Test
    void testBuildAuthorizationUrlContainsRequiredOAuthParams() {
        String state = "secure-random-state-123";
        String authUrl = googleOAuthService.buildAuthorizationUrl(state, null);

        assertNotNull(authUrl);
        assertTrue(authUrl.contains("client_id=test-client-id"));
        assertTrue(authUrl.contains("redirect_uri=http://localhost:5173/auth/callback"));
        assertTrue(authUrl.contains("response_type=code"));
        assertTrue(authUrl.contains("scope=openid%20email%20profile") || authUrl.contains("scope=openid+email+profile"));
        assertTrue(authUrl.contains("state=secure-random-state-123"));
    }

    @Test
    void testExchangeCodeSuccessReturnsVerifiedUserInfo() {
        Map<String, Object> tokenPayload = Map.of("access_token", "ya29.test-access-token", "expires_in", 3600);
        when(restTemplate.postForEntity(eq(properties.getTokenUri()), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenPayload, HttpStatus.OK));

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .sub("google-user-12345")
                .name("Alice Wonder")
                .email("alice@gmail.com")
                .emailVerified(true)
                .build();
        when(restTemplate.exchange(eq(properties.getUserInfoUri()), eq(HttpMethod.GET), any(HttpEntity.class), eq(GoogleUserInfo.class)))
                .thenReturn(new ResponseEntity<>(userInfo, HttpStatus.OK));

        GoogleUserInfo result = googleOAuthService.exchangeCodeForUserInfo("valid-auth-code", null);

        assertNotNull(result);
        assertEquals("alice@gmail.com", result.getEmail());
        assertEquals("Alice Wonder", result.getName());
        assertTrue(result.getEmailVerified());
    }

    @Test
    void testExchangeCodeFailsWhenEmailNotVerified() {
        Map<String, Object> tokenPayload = Map.of("access_token", "ya29.test-token");
        when(restTemplate.postForEntity(eq(properties.getTokenUri()), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenPayload, HttpStatus.OK));

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .sub("google-unverified")
                .name("Fake User")
                .email("unverified@example.com")
                .emailVerified(false)
                .build();
        when(restTemplate.exchange(eq(properties.getUserInfoUri()), eq(HttpMethod.GET), any(HttpEntity.class), eq(GoogleUserInfo.class)))
                .thenReturn(new ResponseEntity<>(userInfo, HttpStatus.OK));

        AuthRegistrationException ex = assertThrows(AuthRegistrationException.class, () ->
                googleOAuthService.exchangeCodeForUserInfo("code-unverified", null));

        assertTrue(ex.getMessage().contains("verified"));
    }
}
