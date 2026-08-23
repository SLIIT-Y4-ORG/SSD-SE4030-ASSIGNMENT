package com.example.userservice.controller;

import com.example.userservice.model.User;
import com.example.userservice.model.UserRole;
import com.example.userservice.dto.UpdateRoleRequest;
import com.example.userservice.exception.ForbiddenException;
import com.example.userservice.service.AuthService;
import com.example.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    
    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        requireAdmin(authHeader);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/patients")
    public ResponseEntity<List<User>> getPatientUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        requireAdminOrReceptionist(authHeader);
        return ResponseEntity.ok(userService.getPatientUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable UUID id) {
        requireSelfOrAdmin(authHeader, id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/by-email")
    public ResponseEntity<User> getUserByEmail(@RequestHeader(value = "Authorization", required = false) String authHeader, @RequestParam String email) {
        User caller = currentUser(authHeader);
        if (caller.getRole() != UserRole.ADMIN && !caller.getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenException("You may only view your own account");
        }
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/{id}/role")
    public ResponseEntity<Map<String, String>> getUserRole(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable UUID id) {
        requireSelfOrAdmin(authHeader, id);
        UserRole role = userService.getUserById(id).getRole();
        return ResponseEntity.ok(Map.of("role", role != null ? role.name() : "UNKNOWN"));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody User user) {
        requireAdmin(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id, @Valid @RequestBody User user) {
        requireSelfOrAdmin(authHeader, id);
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<User> updateUserRole(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        requireAdmin(authHeader);
        return ResponseEntity.ok(userService.updateRole(id, request.role()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable UUID id) {
        requireSelfOrAdmin(authHeader, id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private User currentUser(String authHeader) {
        return authService.getCurrentUser(stripBearer(authHeader));
    }

    private void requireAdmin(String authHeader) {
        if (currentUser(authHeader).getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Administrator role required");
        }
    }

    private void requireAdminOrReceptionist(String authHeader) {
        UserRole role = currentUser(authHeader).getRole();
        if (role != UserRole.ADMIN && role != UserRole.RECEPTIONIST) {
            throw new ForbiddenException("Administrator or receptionist role required");
        }
    }

    private User requireSelfOrAdmin(String authHeader, UUID targetId) {
        User caller = currentUser(authHeader);
        if (caller.getRole() != UserRole.ADMIN && !caller.getId().equals(targetId)) {
            throw new ForbiddenException("You may only access your own account");
        }
        return caller;
    }

    private String stripBearer(String authHeader) {
        if (authHeader == null || authHeader.length() < 8
                || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)
                || authHeader.substring(7).isBlank()) {
            throw new com.example.userservice.exception.AuthRegistrationException("Bearer token required");
        }
        return authHeader.substring(7).trim();
    }
}
