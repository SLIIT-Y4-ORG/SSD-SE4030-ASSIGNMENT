package com.example.userservice.config;

import com.example.userservice.model.User;
import com.example.userservice.model.UserRole;
import com.example.userservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {
    
    private final UserRepository userRepository;
    
    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Create demo doctor if not exists or fix password if null
        Optional<User> existingDoctor = userRepository.findByEmail("demo_doctor@gmail.com");
        
        if (existingDoctor.isPresent() && existingDoctor.get().getPassword() == null) {
            // Update existing user with password
            User doctor = existingDoctor.get();
            doctor.setPassword("Demo@123");
            doctor.setUpdatedAt(LocalDateTime.now());
            userRepository.save(doctor);
            System.out.println("Updated demo_doctor@gmail.com password");
        } else if (existingDoctor.isEmpty()) {
            // Create new demo doctor
            User demoDoctor = User.builder()
                    .id(UUID.randomUUID())
                    .name("Demo Doctor")
                    .email("demo_doctor@gmail.com")
                    .role(UserRole.DOCTOR)
                    .phone("1111111111")
                    .password("Demo@123")
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            userRepository.save(demoDoctor);
            System.out.println("Created demo_doctor@gmail.com with password");
        }
    }
}