package com.example.userservice.config;

import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.PasswordService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
                    user.setPassword(passwordService.hash(user.getPassword()));
                    userRepository.save(user);
                });
    }
}
