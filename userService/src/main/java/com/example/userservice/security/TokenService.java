package com.example.userservice.security;

import com.example.userservice.exception.AuthRegistrationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class TokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;
    private final Clock clock;

    @Autowired
    public TokenService(@Value("${security.token-secret}") String secret) {
        this(secret, Clock.systemUTC());
    }

    TokenService(String secret, Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("SECURITY_TOKEN_SECRET must contain at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public String issue(UUID userId, TokenType type, long version) {
        long lifetime = type == TokenType.ACCESS ? 3600 : 604800;
        String payload = userId + ":" + Instant.now(clock).plusSeconds(lifetime).getEpochSecond()
                + ":" + type + ":" + version;
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + sign(encoded);
    }

    public TokenClaims verify(String token, TokenType requiredType) {
        try {
            if (token == null || token.isBlank() || token.length() > 2048) {
                throw invalidToken();
            }
            String[] parts = token.split("\\.");
            if (parts.length != 2 || !MessageDigest.isEqual(
                    sign(parts[0]).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
                throw invalidToken();
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] claims = payload.split(":");
            if (claims.length != 4 || !requiredType.name().equals(claims[2])
                    || Instant.now(clock).getEpochSecond() >= Long.parseLong(claims[1])) {
                throw invalidToken();
            }
            long version = Long.parseLong(claims[3]);
            if (version < 0) {
                throw invalidToken();
            }
            return new TokenClaims(UUID.fromString(claims[0]), version);
        } catch (AuthRegistrationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw invalidToken();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC is unavailable", ex);
        }
    }

    private AuthRegistrationException invalidToken() {
        return new AuthRegistrationException("Invalid or expired token");
    }

    public enum TokenType { ACCESS, REFRESH }

    public record TokenClaims(UUID userId, long version) {}
}
