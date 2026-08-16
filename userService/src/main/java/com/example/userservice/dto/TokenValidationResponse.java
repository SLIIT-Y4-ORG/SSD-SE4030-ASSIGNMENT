package com.example.userservice.dto;

import com.example.userservice.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Returned by GET /api/auth/validate.
 * Other services (patient, doctor, appointment, payment) call this endpoint
 * to verify a JWT and resolve the caller's userId and role without needing
 * direct Supabase access.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {

    private boolean valid;
    private UUID userId;
    private String email;
    private UserRole role;
    private String message;
}
