package com.example.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private UUID id;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 100)
    private String name;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Size(max = 30)
    private String phone;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 12, max = 128)
    private String password;
    
    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "auth_version")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long authVersion = 0L;

    @Builder.Default
    @Column(name = "refresh_version")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long refreshVersion = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
