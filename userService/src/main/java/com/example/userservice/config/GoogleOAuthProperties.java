package com.example.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.oauth2.google")
@Getter
@Setter
public class GoogleOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri = "http://localhost:5173/auth/callback";
    private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";
    private String tokenUri = "https://oauth2.googleapis.com/token";
    private String userInfoUri = "https://openidconnect.googleapis.com/v1/userinfo";
}
