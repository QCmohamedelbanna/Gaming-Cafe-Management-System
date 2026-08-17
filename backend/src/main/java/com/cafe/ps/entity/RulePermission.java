package com.cafe.ps.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "rule_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rule_permission",
                columnNames = {"rule_id", "permission"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RulePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private AccessRule rule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private Permission permission;
}
