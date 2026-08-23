package com.example.userservice.config;

import com.example.userservice.model.User;
import com.example.userservice.model.UserRole;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminBootstrap implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordService passwordService,
            @Value("${bootstrap.admin.email:}") String email,
            @Value("${bootstrap.admin.password:}") String password,
            @Value("${bootstrap.admin.name:System Administrator}") String name) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.password = password == null ? "" : password;
        this.name = name;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("Admin bootstrap is disabled");
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Bootstrap admin account already exists; leaving it unchanged");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(email)
                .password(passwordService.hash(password))
                .role(UserRole.ADMIN)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
        log.info("Created bootstrap administrator account");
    }
}
