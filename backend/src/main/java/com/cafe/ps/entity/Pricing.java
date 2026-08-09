package com.cafe.ps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "pricing",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"device_type", "session_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_unit", nullable = false)
    private BillingUnit billingUnit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "match_duration_minutes")
    private Integer matchDurationMinutes;

    @Column(name = "warning_before_expiry_minutes")
    private Integer warningBeforeExpiryMinutes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}