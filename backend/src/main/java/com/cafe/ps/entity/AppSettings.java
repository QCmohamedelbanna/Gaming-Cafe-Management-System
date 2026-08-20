package com.cafe.ps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * A single row of admin-editable business rules, replacing what used to be
 * static application.properties values (inventory.prevent-negative,
 * pos.discount.allowed-roles, dashboard.ending-soon-minutes,
 * reservations.no-show-grace-minutes) so they can be changed at runtime
 * from the Settings page instead of requiring a redeploy.
 */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prevent_negative_stock", nullable = false)
    @Builder.Default
    private Boolean preventNegativeStock = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "app_settings_discount_roles",
            joinColumns = @JoinColumn(name = "app_settings_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Set<Role> discountAllowedRoles = new HashSet<>(Set.of(Role.ADMIN, Role.MANAGER));

    @Column(name = "dashboard_ending_soon_minutes", nullable = false)
    @Builder.Default
    private Integer dashboardEndingSoonMinutes = 30;

    @Column(name = "reservations_no_show_grace_minutes", nullable = false)
    @Builder.Default
    private Integer reservationsNoShowGraceMinutes = 20;
}
