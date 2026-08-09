package com.cafe.ps.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GameSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;
    private Integer plannedMinutes;

    // Kept for backward compatibility with the first MVP database.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRateSnapshot;

    @Enumerated(EnumType.STRING)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    private BillingUnit billingUnit;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    private Integer matchDurationMinutesSnapshot;
    private Integer purchasedMatches;
    private Integer completedMatches;
    private LocalDateTime currentMatchStartedAt;
    private LocalDateTime currentMatchExpiresAt;
    private Integer warningBeforeExpiryMinutesSnapshot;
    private Boolean matchExpired;

    @Column(precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;
}
