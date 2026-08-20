package com.cafe.ps.dto;

import com.cafe.ps.entity.BillingUnit;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.SessionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GameSessionResponse(
        Long id,
        DeviceResponse device,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer plannedMinutes,
        BigDecimal hourlyRateSnapshot,
        SessionType sessionType,
        BillingUnit billingUnit,
        BigDecimal unitPriceSnapshot,
        Integer matchDurationMinutesSnapshot,
        Integer purchasedMatches,
        Integer completedMatches,
        LocalDateTime currentMatchStartedAt,
        LocalDateTime currentMatchExpiresAt,
        Integer warningBeforeExpiryMinutesSnapshot,
        Boolean matchExpired,
        BigDecimal finalAmount,
        SessionStatus status
) {
    public static GameSessionResponse from(GameSession session) {
        return new GameSessionResponse(
                session.getId(),
                session.getDevice() == null ? null : DeviceResponse.from(session.getDevice()),
                session.getStartTime(),
                session.getEndTime(),
                session.getPlannedMinutes(),
                session.getHourlyRateSnapshot(),
                session.getSessionType(),
                session.getBillingUnit(),
                session.getUnitPriceSnapshot(),
                session.getMatchDurationMinutesSnapshot(),
                session.getPurchasedMatches(),
                session.getCompletedMatches(),
                session.getCurrentMatchStartedAt(),
                session.getCurrentMatchExpiresAt(),
                session.getWarningBeforeExpiryMinutesSnapshot(),
                session.getMatchExpired(),
                session.getFinalAmount(),
                session.getStatus()
        );
    }
}
