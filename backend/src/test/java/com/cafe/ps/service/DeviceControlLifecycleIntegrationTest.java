package com.cafe.ps.service;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import com.cafe.ps.dto.PowerCommandResult;
import com.cafe.ps.entity.BillStatus;
import com.cafe.ps.entity.BillingUnit;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceControlProvider;
import com.cafe.ps.entity.DevicePowerState;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.PaymentMethod;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.repository.BillRepository;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import com.cafe.ps.repository.PaymentRepository;
import com.cafe.ps.repository.PricingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Uses the real Spring session/billing services and MySQL database while
 * replacing only the external provider boundary. No test calls Tuya Cloud.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false"
        }
)
class DeviceControlLifecycleIntegrationTest extends AbstractMySQLIntegrationTest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private DeviceControlService deviceControlService;

    @BeforeEach
    void resetProviderMock() {
        reset(deviceControlService);
    }

    @Test
    void sessionStartCommitsThenRequestsPowerOn() {
        when(deviceControlService.powerOn(any(Device.class)))
                .thenReturn(success(DevicePowerState.ON));

        Device device = saveAvailableControlledDevice();
        seedHourlyPricing();

        GameSession session = sessionService.start(
                device.getId(), SessionType.SINGLE, null, null
        );

        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.ACTIVE);
        verify(deviceControlService, times(1)).powerOn(any(Device.class));
        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getPhysicalPowerStatus())
                .isEqualTo(DevicePowerState.ON);
    }

    @Test
    void failedPowerOnDoesNotRemoveTheStartedSession() {
        when(deviceControlService.powerOn(any(Device.class)))
                .thenReturn(PowerCommandResult.failure(
                        DeviceControlProvider.TUYA,
                        DevicePowerState.OFFLINE,
                        "Tuya unavailable"
                ));

        Device device = saveAvailableControlledDevice();
        seedHourlyPricing();

        GameSession session = sessionService.start(
                device.getId(), SessionType.SINGLE, null, null
        );

        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.ACTIVE);
        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getStatus())
                .isEqualTo(DeviceStatus.PLAYING);
        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getLastControlError())
                .isEqualTo("Tuya unavailable");
    }

    @Test
    void manualStopRequestsPowerOffAfterFinalization() {
        when(deviceControlService.powerOn(any(Device.class)))
                .thenReturn(success(DevicePowerState.ON));
        when(deviceControlService.powerOff(any(Device.class)))
                .thenReturn(success(DevicePowerState.OFF));

        Device device = saveAvailableControlledDevice();
        seedHourlyPricing();
        GameSession session = sessionService.start(device.getId(), SessionType.SINGLE, null, null);

        sessionService.stop(session.getId(), "cashier");

        verify(deviceControlService, times(1)).powerOff(any(Device.class));
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.COMPLETED);
        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getStatus())
                .isEqualTo(DeviceStatus.AVAILABLE);
        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getPhysicalPowerStatus())
                .isEqualTo(DevicePowerState.OFF);
        assertThat(billRepository.findBySessionId(session.getId())).isPresent();
    }

    @Test
    void automaticExpiryRequestsPowerOffAndKeepsThePendingBill() {
        when(deviceControlService.powerOn(any(Device.class)))
                .thenReturn(success(DevicePowerState.ON));
        when(deviceControlService.powerOff(any(Device.class)))
                .thenReturn(success(DevicePowerState.OFF));

        Device device = saveAvailableControlledDevice();
        seedHourlyPricing();
        GameSession session = sessionService.start(device.getId(), SessionType.SINGLE, 30, null);
        session.setStartTime(LocalDateTime.now().minusHours(1));
        sessionRepository.save(session);

        sessionService.maintainActiveSessions();

        verify(deviceControlService, times(1)).powerOff(any(Device.class));
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.COMPLETED);
        assertThat(billRepository.findBySessionId(session.getId()).orElseThrow().getStatus())
                .isEqualTo(BillStatus.PENDING_PAYMENT);
    }

    @Test
    void repeatedFinalizationSchedulesOnlyOnePowerOff() {
        when(deviceControlService.powerOff(any(Device.class)))
                .thenReturn(success(DevicePowerState.OFF));

        Device device = saveAvailableControlledDevice();
        GameSession session = saveActiveMatch(device);

        billingService.finalizeSession(session.getId(), LocalDateTime.now(), true);
        billingService.finalizeSession(session.getId(), LocalDateTime.now(), true);

        verify(deviceControlService, times(1)).powerOff(any(Device.class));
        assertThat(billRepository.findBySessionId(session.getId())).hasValueSatisfying(
                bill -> assertThat(bill.getStatus()).isEqualTo(BillStatus.PENDING_PAYMENT)
        );
        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void checkoutStillPaysWhenPowerOffFails() {
        when(deviceControlService.powerOff(any(Device.class)))
                .thenReturn(PowerCommandResult.failure(
                        DeviceControlProvider.TUYA,
                        DevicePowerState.OFFLINE,
                        "Tuya timeout"
                ));

        Device device = saveAvailableControlledDevice();
        GameSession session = saveActiveMatch(device);

        billingService.checkoutSession(
                session.getId(), PaymentMethod.CASH, new BigDecimal("10.00"), "Admin"
        );

        assertThat(billRepository.findBySessionId(session.getId()).orElseThrow().getStatus())
                .isEqualTo(BillStatus.PAID);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getLastControlError())
                .isEqualTo("Tuya timeout");
    }

    @Test
    void softwareOnlyDeviceDoesNotInvokeHardwareProvider() {
        Device device = deviceRepository.save(Device.builder()
                .name("SOFTWARE-ONLY-" + UUID.randomUUID())
                .type(DeviceType.PS4)
                .status(DeviceStatus.AVAILABLE)
                .active(true)
                .deleted(false)
                .build());
        seedHourlyPricing();

        sessionService.start(device.getId(), SessionType.SINGLE, null, null);

        verifyNoInteractions(deviceControlService);
    }

    private Device saveAvailableControlledDevice() {
        return deviceRepository.save(Device.builder()
                .name("CONTROLLED-" + UUID.randomUUID())
                .type(DeviceType.PS4)
                .status(DeviceStatus.AVAILABLE)
                .active(true)
                .deleted(false)
                .controlProvider(DeviceControlProvider.TUYA)
                .controllerDeviceId("tuya-" + UUID.randomUUID().toString().replace("-", ""))
                .controllerPowerCode("switch_1")
                .powerControlEnabled(true)
                .physicalPowerStatus(DevicePowerState.UNKNOWN)
                .build());
    }

    private GameSession saveActiveMatch(Device device) {
        return sessionRepository.save(GameSession.builder()
                .device(device)
                .startTime(LocalDateTime.now().minusMinutes(15))
                .hourlyRateSnapshot(new BigDecimal("10.00"))
                .unitPriceSnapshot(new BigDecimal("10.00"))
                .sessionType(SessionType.MATCH)
                .billingUnit(BillingUnit.MATCH)
                .purchasedMatches(1)
                .completedMatches(0)
                .matchExpired(false)
                .status(SessionStatus.ACTIVE)
                .build());
    }

    private void seedHourlyPricing() {
        pricingRepository.save(com.cafe.ps.entity.Pricing.builder()
                .deviceType(DeviceType.PS4)
                .sessionType(SessionType.SINGLE)
                .billingUnit(BillingUnit.HOUR)
                .price(new BigDecimal("40.00"))
                .active(true)
                .build());
    }

    private static PowerCommandResult success(DevicePowerState state) {
        return new PowerCommandResult(
                true,
                DeviceControlProvider.TUYA,
                state,
                "stubbed provider result",
                Instant.now()
        );
    }
}
