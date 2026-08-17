package com.cafe.ps.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.CASHIER;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rule_id")
    private AccessRule rule;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    @PreUpdate
    private void normalizeDefaults() {
        if (username != null) username = username.trim().toLowerCase();
        if (displayName == null || displayName.isBlank()) displayName = username;
        if (role == null) role = Role.CASHIER;
        if (active == null) active = true;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
