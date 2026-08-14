package com.cafe.ps.service;

import com.cafe.ps.dto.DashboardSummary;
import com.cafe.ps.entity.*;
import com.cafe.ps.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final DeviceRepository deviceRepository;
    private final GameSessionRepository sessionRepository;
    private final PricingService pricingService;
    private final BillingService billingService;
    private final ReportService reportService;

    @Value("${spring.task.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Transactional
    public GameSession start(Long deviceId, SessionType sessionType, Integer plannedMinutes, Integer matchCount) {
        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("Device not found"));
        assertDeviceAvailable(device);

        Pricing pricing = pricingService.getPricing(device.getType(), sessionType);
        LocalDateTime now = LocalDateTime.now();
        GameSession.GameSessionBuilder builder =
                GameSession.builder().device(device).startTime(now)
                        .sessionType(sessionType).billingUnit(pricing.getBillingUnit())
                        .unitPriceSnapshot(pricing.getPrice())
                        .hourlyRateSnapshot(pricing.getBillingUnit() == BillingUnit.HOUR ?
                                pricing.getPrice() : BigDecimal.ZERO).status(SessionStatus.ACTIVE);

        if (sessionType == SessionType.MATCH) {
            int duration = pricing.getMatchDurationMinutes() == null ? 15 : pricing.getMatchDurationMinutes();
            int count = matchCount == null ? 1 : matchCount;
            builder.plannedMinutes(null)
                    .matchDurationMinutesSnapshot(duration)
                    .warningBeforeExpiryMinutesSnapshot(
                            pricing.getWarningBeforeExpiryMinutes()
                    )
                    .purchasedMatches(count).completedMatches(0)
                    .currentMatchStartedAt(now)
                    .currentMatchExpiresAt(now.plusMinutes(duration))
                    .matchExpired(false);
        } else {
            builder.plannedMinutes(plannedMinutes);
        }

        device.setStatus(DeviceStatus.PLAYING);
        deviceRepository.save(device);
        return sessionRepository.save(builder.build());
    }

    @Transactional
    public GameSession stop(Long sessionId) {
        billingService.finalizeSession(sessionId, LocalDateTime.now());
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    @Transactional
    public GameSession extend(Long sessionId, int minutes) {
        GameSession session = getActive(sessionId);
        if (session.getSessionType() == SessionType.MATCH) {
            throw new IllegalStateException("Use match controls for MATCH sessions");
        }
        if (session.getPlannedMinutes() == null) {
            throw new IllegalStateException("Open sessions do not need extension");
        }
        session.setPlannedMinutes(session.getPlannedMinutes() + minutes);
        return sessionRepository.save(session);
    }

    @Transactional
    public GameSession finishCurrentMatch(Long sessionId) {
        GameSession session = getActiveMatch(sessionId);
        int completed = valueOrZero(session.getCompletedMatches()) + 1;
        session.setCompletedMatches(completed);
        session.setMatchExpired(false);

        if (completed >= valueOrOne(session.getPurchasedMatches())) {
            billingService.finalizeSession(sessionId, LocalDateTime.now());
            return sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        }

        LocalDateTime now = LocalDateTime.now();
        session.setCurrentMatchStartedAt(now);
        session.setCurrentMatchExpiresAt(now.plusMinutes(valueOrDefault(session.getMatchDurationMinutesSnapshot(), 15)));
        return sessionRepository.save(session);
    }

    @Transactional
    public GameSession addMatch(Long sessionId) {
        GameSession session = getActiveMatch(sessionId);
        session.setPurchasedMatches(valueOrOne(session.getPurchasedMatches()) + 1);
        return sessionRepository.save(session);
    }

    public List<GameSession> activeSessions() {
        return sessionRepository.findAll().stream().filter(s -> s.getStatus() == SessionStatus.ACTIVE).toList();
    }

    public DashboardSummary todaySummary() {
        return reportService.dashboardSummary();
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void maintainActiveSessions() {
        if (!schedulingEnabled) return;
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.findAll().stream().filter(s -> s.getStatus() == SessionStatus.ACTIVE).forEach(s -> {
            if (s.getSessionType() == SessionType.MATCH
                    || s.getBillingUnit() == BillingUnit.MATCH) {
                if (s.getCurrentMatchExpiresAt() != null
                        && !s.getCurrentMatchExpiresAt().isAfter(now)
                        && !Boolean.TRUE.equals(s.getMatchExpired())) {
                    int purchased = valueOrOne(s.getPurchasedMatches());
                    int completed = valueOrZero(s.getCompletedMatches());

                    if (completed + 1 >= purchased) {
                        s.setCompletedMatches(purchased);
                        s.setMatchExpired(false);
                        billingService.finalizeSession(
                                s.getId(),
                                s.getCurrentMatchExpiresAt(),
                                true
                        );
                    } else {
                        s.setMatchExpired(true);
                        sessionRepository.save(s);
                    }
                }
                return;
            }
            if (s.getPlannedMinutes() != null && !s.getStartTime().plusMinutes(s.getPlannedMinutes()).isAfter(now)) {
                billingService.finalizeSession(
                        s.getId(),
                        s.getStartTime().plusMinutes(s.getPlannedMinutes()),
                        true
                );
            }
        });
    }

    private void assertDeviceAvailable(Device device) {
        if (Boolean.FALSE.equals(device.getActive())
                || Boolean.TRUE.equals(device.getDeleted())) {
            throw new IllegalStateException("Device is inactive");
        }
        if (device.getStatus() == DeviceStatus.PLAYING || sessionRepository.findFirstByDeviceIdAndStatusOrderByStartTimeDesc(device.getId(), SessionStatus.ACTIVE).isPresent()) {
            throw new IllegalStateException("Device already has an active session");
        }
        if (device.getStatus() == DeviceStatus.MAINTENANCE || device.getStatus() == DeviceStatus.OFFLINE) {
            throw new IllegalStateException("Device is not available");
        }
    }

    private GameSession getActive(Long sessionId) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (session.getStatus() != SessionStatus.ACTIVE) throw new IllegalStateException("Session is not active");
        return session;
    }

    private GameSession getActiveMatch(Long sessionId) {
        GameSession session = getActive(sessionId);
        if (session.getSessionType() != SessionType.MATCH)
            throw new IllegalStateException("Session is not a MATCH session");
        return session;
    }

    private static int valueOrZero(Integer n) {
        return n == null ? 0 : n;
    }

    private static int valueOrOne(Integer n) {
        return n == null || n < 1 ? 1 : n;
    }

    private static int valueOrDefault(Integer n, int d) {
        return n == null ? d : n;
    }
}
