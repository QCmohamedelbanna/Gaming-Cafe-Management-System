package com.cafe.ps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "access_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_access_rule_name",
                columnNames = "name"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "system_rule", nullable = false)
    @Builder.Default
    private Boolean systemRule = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", length = 20)
    private Role systemRole;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void normalizeDefaults() {
        if (name != null) name = name.trim();
        if (description != null) description = description.trim();
        if (systemRule == null) systemRule = false;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
