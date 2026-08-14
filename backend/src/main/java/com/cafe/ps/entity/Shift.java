package com.cafe.ps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cashier_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.OPEN;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal openingCash = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(precision = 12, scale = 2)
    private BigDecimal actualCash;

    @Column(precision = 12, scale = 2)
    private BigDecimal cashDifference;

    @Column(length = 500)
    private String closingNote;
}
