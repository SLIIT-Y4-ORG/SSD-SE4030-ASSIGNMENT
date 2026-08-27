package com.example.userservice.security;

import com.example.userservice.config.GoogleOAuthProperties;
import com.example.userservice.dto.GoogleUserInfo;
import com.example.userservice.exception.AuthRegistrationException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class GoogleOAuthService {

    private final GoogleOAuthProperties properties;
    private final RestTemplate restTemplate;

    public GoogleOAuthService(GoogleOAuthProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public String buildAuthorizationUrl(String state, String customRedirectUri) {
        String redirectUri = resolveRedirectUri(customRedirectUri);
        return UriComponentsBuilder.fromHttpUrl(properties.getAuthorizationUri())
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state != null ? state : "")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "select_account")
                .encode()
                .toUriString();
    }

    public GoogleUserInfo exchangeCodeForUserInfo(String code, String customRedirectUri) {
        if (code == null || code.isBlank()) {
            throw new AuthRegistrationException("Authorization code cannot be empty");
        }

        String redirectUri = resolveRedirectUri(customRedirectUri);

        // 1. Exchange authorization code for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(body, headers);

        Map<String, Object> tokenResponse;
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    properties.getTokenUri(),
                    tokenRequest,
                    Map.class
            );
            tokenResponse = response.getBody();
        } catch (RestClientException e) {
            throw new AuthRegistrationException("Failed to exchange authorization code with Google: " + e.getMessage());
        }

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new AuthRegistrationException("Google token response did not contain access token");
        }

        String accessToken = (String) tokenResponse.get("access_token");

        // 2. Fetch verified user claims from Google OpenID Connect UserInfo endpoint
        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> userInfoRequest = new HttpEntity<>(userInfoHeaders);

        GoogleUserInfo userInfo;
        try {
            ResponseEntity<GoogleUserInfo> userInfoEntity = restTemplate.exchange(
                    properties.getUserInfoUri(),
                    HttpMethod.GET,
                    userInfoRequest,
                    GoogleUserInfo.class
            );
            userInfo = userInfoEntity.getBody();
        } catch (RestClientException e) {
            throw new AuthRegistrationException("Failed to retrieve Google user profile: " + e.getMessage());
        }

        if (userInfo == null || userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new AuthRegistrationException("Failed to obtain verified email from Google identity provider");
        }

        if (!Boolean.TRUE.equals(userInfo.getEmailVerified())) {
            throw new AuthRegistrationException("Google email address is not verified");
        }

        return userInfo;
    }

    public String resolveRedirectUri(String customRedirectUri) {
        if (customRedirectUri != null && !customRedirectUri.isBlank()) {
            return customRedirectUri.trim();
        }
        return properties.getRedirectUri();
    }
}
