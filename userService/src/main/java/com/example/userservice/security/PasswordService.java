package com.example.userservice.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordService {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String PREFIX = "pbkdf2-sha256";
    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(password.toCharArray(), salt, ITERATIONS);
        return String.join("$", PREFIX, String.valueOf(ITERATIONS),
                Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(derived));
    }

    public boolean matches(String password, String encoded) {
        if (password == null || encoded == null || !encoded.startsWith(PREFIX + "$")) {
            return false;
        }
        try {
            String[] parts = encoded.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(password.toCharArray(), salt, iterations));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public boolean isHash(String value) {
        return value != null && value.startsWith(PREFIX + "$");
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 is unavailable", ex);
        }
    }
}
