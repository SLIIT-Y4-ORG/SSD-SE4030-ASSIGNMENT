package com.example.userservice.config;

import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.PasswordService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LegacyPasswordMigration implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public LegacyPasswordMigration(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Override
    public void run(String... args) {
        // Upgrade records created by the vulnerable version, then never retain plaintext.
        userRepository.findAll().stream()
                .filter(user -> user.getPassword() != null && !passwordService.isHash(user.getPassword()))
                .forEach(user -> {
                    String legacyPassword = user.getPassword();
                    if (legacyPassword.isEmpty() || legacyPassword.length() > 128) {
                        // Remove unusable plaintext while requiring an administrator-led
                        // password recovery before this account can authenticate again.
                        user.setPassword(passwordService.hash(UUID.randomUUID() + "-disabled"));
                        user.setEnabled(false);
                    } else {
                        user.setPassword(passwordService.hashLegacyPassword(legacyPassword));
                    }
                    userRepository.save(user);
                });
    }
}
