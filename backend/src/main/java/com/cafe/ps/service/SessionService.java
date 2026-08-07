package com.cafe.ps.service;

import com.cafe.ps.dto.DashboardSummary;
import com.cafe.ps.entity.*;
import com.cafe.ps.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final DeviceRepository deviceRepository;
    private final GameSessionRepository sessionRepository;
    private final PricingRepository pricingRepository;

    @Transactional
    public GameSession start(Long deviceId, SessionType sessionType, Integer plannedMinutes, Integer matchCount) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
        assertDeviceAvailable(device);

        Pricing pricing = pricingRepository.findByDeviceTypeAndSessionType(device.getType(), sessionType)
                .orElseThrow(() -> new IllegalStateException("Pricing is not configured for " + device.getType() + " / " + sessionType));

        LocalDateTime now = LocalDateTime.now();
        GameSession.GameSessionBuilder builder = GameSession.builder()
                .device(device)
                .startTime(now)
                .sessionType(sessionType)
                .billingUnit(pricing.getBillingUnit())
                .unitPriceSnapshot(pricing.getPrice())
                .hourlyRateSnapshot(pricing.getBillingUnit() == BillingUnit.HOUR ? pricing.getPrice() : device.getHourlyRate())
                .status(SessionStatus.ACTIVE);

        if (sessionType == SessionType.MATCH) {
            int duration = pricing.getMatchDurationMinutes() == null ? 15 : pricing.getMatchDurationMinutes();
            int count = matchCount == null ? 1 : matchCount;
            builder.plannedMinutes(null)
                    .matchDurationMinutesSnapshot(duration)
                    .purchasedMatches(count)
                    .completedMatches(0)
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
        GameSession session = getActive(sessionId);
        return completeAt(session, LocalDateTime.now());
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
            return completeAt(session, LocalDateTime.now());
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
        LocalDate today = LocalDate.now();
        List<GameSession> sessions = sessionRepository.findByStartTimeBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long active = sessions.stream().filter(s -> s.getStatus() == SessionStatus.ACTIVE).count();
        long completed = sessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
        BigDecimal revenue = sessions.stream().map(GameSession::getFinalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardSummary(deviceRepository.count(), active, completed, revenue);
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void maintainActiveSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.findAll().stream().filter(s -> s.getStatus() == SessionStatus.ACTIVE).forEach(s -> {
            if (s.getSessionType() == SessionType.MATCH) {
                if (s.getCurrentMatchExpiresAt() != null && !s.getCurrentMatchExpiresAt().isAfter(now) && !Boolean.TRUE.equals(s.getMatchExpired())) {
                    s.setMatchExpired(true);
                    sessionRepository.save(s);
                }
                return;
            }
            if (s.getPlannedMinutes() != null && !s.getStartTime().plusMinutes(s.getPlannedMinutes()).isAfter(now)) {
                completeAt(s, s.getStartTime().plusMinutes(s.getPlannedMinutes()));
            }
        });
    }

    private void assertDeviceAvailable(Device device) {
        if (device.getStatus() == DeviceStatus.PLAYING ||
                sessionRepository.findFirstByDeviceIdAndStatusOrderByStartTimeDesc(device.getId(), SessionStatus.ACTIVE).isPresent()) {
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
        if (session.getSessionType() != SessionType.MATCH) throw new IllegalStateException("Session is not a MATCH session");
        return session;
    }

    private GameSession completeAt(GameSession session, LocalDateTime end) {
        BigDecimal amount;
        if (session.getSessionType() == SessionType.MATCH || session.getBillingUnit() == BillingUnit.MATCH) {
            amount = nz(session.getUnitPriceSnapshot()).multiply(BigDecimal.valueOf(valueOrOne(session.getPurchasedMatches()))).setScale(2, RoundingMode.HALF_UP);
        } else {
            long seconds = Math.max(1, ChronoUnit.SECONDS.between(session.getStartTime(), end));
            if (session.getPlannedMinutes() != null) seconds = Math.min(seconds, session.getPlannedMinutes() * 60L);
            BigDecimal hours = BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
            BigDecimal rate = session.getUnitPriceSnapshot() != null ? session.getUnitPriceSnapshot() : session.getHourlyRateSnapshot();
            amount = nz(rate).multiply(hours).setScale(2, RoundingMode.HALF_UP);
        }

        session.setEndTime(end);
        session.setFinalAmount(amount);
        session.setStatus(SessionStatus.COMPLETED);
        session.getDevice().setStatus(DeviceStatus.AVAILABLE);
        deviceRepository.save(session.getDevice());
        return sessionRepository.save(session);
    }

    private static BigDecimal nz(BigDecimal n) { return n == null ? BigDecimal.ZERO : n; }
    private static int valueOrZero(Integer n) { return n == null ? 0 : n; }
    private static int valueOrOne(Integer n) { return n == null || n < 1 ? 1 : n; }
    private static int valueOrDefault(Integer n, int d) { return n == null ? d : n; }
}
