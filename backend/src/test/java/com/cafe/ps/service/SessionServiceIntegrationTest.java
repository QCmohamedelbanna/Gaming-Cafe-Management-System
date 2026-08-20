package com.cafe.ps.service;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import com.cafe.ps.entity.Bill;
import com.cafe.ps.entity.BillStatus;
import com.cafe.ps.entity.BillingUnit;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.Pricing;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.repository.BillRepository;
import com.cafe.ps.repository.CafeOrderRepository;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import com.cafe.ps.repository.PricingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises session pricing, planned-time caps, match billing progression,
 * and the scheduled auto-completion sweep directly against SessionService
 * (bypassing the @Scheduled trigger, since scheduling is disabled for tests).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false"
        }
)
class SessionServiceIntegrationTest extends AbstractMySQLIntegrationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Autowired
    private SessionService sessionService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private CafeOrderRepository orderRepository;

    @Autowired
    private BillingService billingService;

    @BeforeEach
    void cleanDatabase() {
        billRepository.deleteAll();
        orderRepository.deleteAll();
        sessionRepository.deleteAll();
        pricingRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void hourlySessionChargesExactRateForAWholeHour() {
        Device device = saveDevice(DeviceType.PS4);
        seedHourlyPricing(DeviceType.PS4, SessionType.SINGLE, "40.00");

        GameSession session = sessionService.start(device.getId(), SessionType.SINGLE, null, null);
        completeManually(session, START.plusHours(1));

        assertThat(reload(session).getFinalAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void hourlySessionRoundsSubHourDurationToTheNearestCent() {
        Device device = saveDevice(DeviceType.PS4);
        seedHourlyPricing(DeviceType.PS4, SessionType.SINGLE, "40.00");

        GameSession session = sessionService.start(device.getId(), SessionType.SINGLE, null, null);
        // 25 minutes at 40.00/hr = 16.666...  -> rounds to 16.67
        completeManually(session, START.plusMinutes(25));

        assertThat(reload(session).getFinalAmount()).isEqualByComparingTo("16.67");
    }

    @Test
    void plannedSessionCapsBillingEvenWhenCompletedLate() {
        Device device = saveDevice(DeviceType.PS4);
        seedHourlyPricing(DeviceType.PS4, SessionType.SINGLE, "40.00");

        GameSession session = sessionService.start(device.getId(), SessionType.SINGLE, 30, null);
        // Completed a full hour after start, well past the 30-minute plan.
        completeManually(session, START.plusHours(1));

        assertThat(reload(session).getFinalAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void matchSessionChargesUnitPriceTimesPurchasedMatchesRegardlessOfElapsedTime() {
        Device device = saveDevice(DeviceType.PS4);
        seedMatchPricing(DeviceType.PS4, "15.00", 15, 2);

        GameSession session = sessionService.start(device.getId(), SessionType.MATCH, null, 3);
        completeManually(session, START.plusHours(5));

        assertThat(reload(session).getFinalAmount()).isEqualByComparingTo("45.00");
    }

    @Test
    void finishingEachMatchAdvancesUntilTheFinalMatchCompletesTheSession() {
        Device device = saveDevice(DeviceType.PS4);
        seedMatchPricing(DeviceType.PS4, "15.00", 15, 2);

        GameSession session = sessionService.start(device.getId(), SessionType.MATCH, null, 2);

        GameSession afterFirst = sessionService.finishCurrentMatch(session.getId());
        assertThat(afterFirst.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(afterFirst.getCompletedMatches()).isEqualTo(1);

        GameSession afterSecond = sessionService.finishCurrentMatch(session.getId());
        assertThat(afterSecond.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(afterSecond.getFinalAmount()).isEqualByComparingTo("30.00");
        assertThat(billRepository.findBySessionId(session.getId())).isPresent();
    }

    @Test
    void addingAMatchExtendsThePurchasedCountWithoutFinalizing() {
        Device device = saveDevice(DeviceType.PS4);
        seedMatchPricing(DeviceType.PS4, "15.00", 15, 2);

        GameSession session = sessionService.start(device.getId(), SessionType.MATCH, null, 1);
        GameSession extended = sessionService.addMatch(session.getId());

        assertThat(extended.getPurchasedMatches()).isEqualTo(2);
        assertThat(extended.getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    void plannedSessionPastDueIsAutomaticallyCompletedWithAPendingBill() {
        Device device = saveDevice(DeviceType.PS4);
        seedHourlyPricing(DeviceType.PS4, SessionType.SINGLE, "40.00");
        GameSession session = sessionService.start(device.getId(), SessionType.SINGLE, 30, null);
        backdateStart(session, LocalDateTime.now().minusHours(1));

        sessionService.maintainActiveSessions();

        GameSession completed = reload(session);
        assertThat(completed.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.getFinalAmount()).isEqualByComparingTo("20.00");
        Bill bill = billRepository.findBySessionId(session.getId()).orElseThrow();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PENDING_PAYMENT);
        assertThat(bill.getAutomaticExpiry()).isTrue();
        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getStatus())
                .isEqualTo(DeviceStatus.AVAILABLE);
    }

    @Test
    void openEndedSessionIsNeverAutomaticallyCompleted() {
        Device device = saveDevice(DeviceType.PS4);
        seedHourlyPricing(DeviceType.PS4, SessionType.SINGLE, "40.00");
        GameSession session = sessionService.start(device.getId(), SessionType.SINGLE, null, null);
        backdateStart(session, LocalDateTime.now().minusDays(1));

        sessionService.maintainActiveSessions();

        assertThat(reload(session).getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(billRepository.findBySessionId(session.getId())).isEmpty();
    }

    @Test
    void expiredFinalMatchIsAutomaticallyCompletedByTheSweep() {
        Device device = saveDevice(DeviceType.PS4);
        seedMatchPricing(DeviceType.PS4, "15.00", 15, 2);
        GameSession session = sessionService.start(device.getId(), SessionType.MATCH, null, 1);
        backdateMatchExpiry(session, LocalDateTime.now().minusMinutes(1));

        sessionService.maintainActiveSessions();

        GameSession completed = reload(session);
        assertThat(completed.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(billRepository.findBySessionId(session.getId())).isPresent();
    }

    @Test
    void expiredNonFinalMatchIsFlaggedButNotCompleted() {
        Device device = saveDevice(DeviceType.PS4);
        seedMatchPricing(DeviceType.PS4, "15.00", 15, 2);
        GameSession session = sessionService.start(device.getId(), SessionType.MATCH, null, 2);
        backdateMatchExpiry(session, LocalDateTime.now().minusMinutes(1));

        sessionService.maintainActiveSessions();

        GameSession stillActive = reload(session);
        assertThat(stillActive.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(stillActive.getMatchExpired()).isTrue();
    }

    @Test
    void startingASessionOnADeviceThatAlreadyHasOneFails() {
        Device device = saveDevice(DeviceType.PS4);
        seedHourlyPricing(DeviceType.PS4, SessionType.SINGLE, "40.00");
        sessionService.start(device.getId(), SessionType.SINGLE, null, null);

        assertThatThrownBy(() -> sessionService.start(device.getId(), SessionType.SINGLE, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active session");
    }

    private Device saveDevice(DeviceType type) {
        return deviceRepository.save(Device.builder()
                .name("SESSION-TEST-" + java.util.UUID.randomUUID())
                .type(type)
                .status(DeviceStatus.AVAILABLE)
                .active(true)
                .deleted(false)
                .build());
    }

    private void seedHourlyPricing(DeviceType deviceType, SessionType sessionType, String price) {
        pricingRepository.save(Pricing.builder()
                .deviceType(deviceType)
                .sessionType(sessionType)
                .billingUnit(BillingUnit.HOUR)
                .price(new BigDecimal(price))
                .active(true)
                .build());
    }

    private void seedMatchPricing(
            DeviceType deviceType,
            String price,
            int matchDurationMinutes,
            int warningBeforeExpiryMinutes
    ) {
        pricingRepository.save(Pricing.builder()
                .deviceType(deviceType)
                .sessionType(SessionType.MATCH)
                .billingUnit(BillingUnit.MATCH)
                .price(new BigDecimal(price))
                .matchDurationMinutes(matchDurationMinutes)
                .warningBeforeExpiryMinutes(warningBeforeExpiryMinutes)
                .active(true)
                .build());
    }

    private void completeManually(GameSession session, LocalDateTime endTime) {
        backdateStart(session, START);
        // Finalizes directly through BillingService so this isolates the price
        // calculation itself (the checkout/payment path is covered separately
        // in CheckoutServiceIntegrationTest).
        billingService.finalizeSession(session.getId(), endTime, false);
    }

    private void backdateStart(GameSession session, LocalDateTime startTime) {
        GameSession managed = reload(session);
        managed.setStartTime(startTime);
        sessionRepository.save(managed);
    }

    private void backdateMatchExpiry(GameSession session, LocalDateTime expiresAt) {
        GameSession managed = reload(session);
        managed.setCurrentMatchExpiresAt(expiresAt);
        sessionRepository.save(managed);
    }

    private GameSession reload(GameSession session) {
        return sessionRepository.findById(session.getId()).orElseThrow();
    }
}
