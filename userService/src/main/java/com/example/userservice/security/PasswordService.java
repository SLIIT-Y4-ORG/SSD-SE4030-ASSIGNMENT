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
    private static final int ITERATIONS = 600_000;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String PREFIX = "pbkdf2-sha256";
    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        if (password == null || password.length() < 12 || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must contain between 12 and 128 characters");
        }
        return hashAcceptedPassword(password);
    }

    public String hashLegacyPassword(String password) {
        if (password == null || password.isEmpty() || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Legacy password cannot be migrated safely");
        }
        return hashAcceptedPassword(password);
    }

    private String hashAcceptedPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(password.toCharArray(), salt, ITERATIONS);
        return String.join("$", PREFIX, String.valueOf(ITERATIONS),
                Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(derived));
    }

    public boolean matches(String password, String encoded) {
        if (password == null || password.length() > MAX_PASSWORD_LENGTH
                || encoded == null || !encoded.startsWith(PREFIX + "$")) {
            return false;
        }
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (iterations < 100_000 || iterations > ITERATIONS || salt.length != SALT_LENGTH
                    || expected.length != KEY_LENGTH / 8) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(password.toCharArray(), salt, iterations));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public boolean isHash(String value) {
        return value != null && value.startsWith(PREFIX + "$");
    }

    public boolean needsUpgrade(String encoded) {
        if (!isHash(encoded)) {
            return true;
        }
        try {
            String[] parts = encoded.split("\\$");
            return parts.length != 4 || Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (RuntimeException ex) {
            return true;
        }
    }

    public void performDummyVerification(String password) {
        byte[] fixedSalt = new byte[SALT_LENGTH];
        derive((password == null ? "" : password).toCharArray(), fixedSalt, ITERATIONS);
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 is unavailable", ex);
        }
    }
}
