package com.example.userservice.dto;

import com.example.userservice.model.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull UserRole role) {
}
