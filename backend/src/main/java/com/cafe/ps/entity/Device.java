package com.cafe.ps.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    /**
     * Kept for databases created before pricing became independent from a
     * device. New devices use the default only to satisfy the legacy schema;
     * session prices come from the Pricing entity.
     */
    @JsonIgnore
    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal legacyHourlyRate = BigDecimal.ZERO;

    /**
     * Administrative availability switch. A null value is treated as active
     * for devices created before this field was introduced.
     */
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Soft deletion marker. Deleted devices stay available to historical
     * sessions and billing records but are hidden from current operations.
     */
    @JsonIgnore
    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(length = 500)
    private String maintenanceNote;
}
