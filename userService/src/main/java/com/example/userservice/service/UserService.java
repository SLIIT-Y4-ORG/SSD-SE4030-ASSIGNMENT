package com.example.userservice.service;

import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.ForbiddenException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.model.User;
import com.example.userservice.model.UserRole;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.PasswordService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    
    public UserService(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getPatientUsers() {
        return userRepository.findAllByRole(UserRole.PATIENT);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    public User createUser(User user) {
        String email = normalizeEmail(user.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(user.getEmail());
        }
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        if (user.getRole() == null) {
            user.setRole(UserRole.PATIENT);
        }
        user.setEnabled(true);
        user.setAuthVersion(0L);
        user.setRefreshVersion(0L);
        user.setPassword(passwordService.hash(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(UUID id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        String email = normalizeEmail(updatedUser.getEmail());
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        user.setName(updatedUser.getName());
        user.setEmail(email);
        user.setPhone(updatedUser.getPhone());

        return userRepository.save(user);
    }

    public User updateRole(UUID id, UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (user.getRole() == UserRole.ADMIN
                && role != UserRole.ADMIN
                && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ForbiddenException("The final administrator cannot be demoted");
        }
        user.setRole(role);
        return userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (user.getRole() == UserRole.ADMIN && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ForbiddenException("The final administrator cannot be deleted");
        }
        userRepository.deleteById(id);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
