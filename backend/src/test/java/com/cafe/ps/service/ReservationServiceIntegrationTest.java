package com.cafe.ps.service;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import com.cafe.ps.dto.CreateReservationRequest;
import com.cafe.ps.entity.BillingUnit;
import com.cafe.ps.entity.Customer;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.Pricing;
import com.cafe.ps.entity.Reservation;
import com.cafe.ps.entity.ReservationStatus;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.repository.CustomerRepository;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import com.cafe.ps.repository.PricingRepository;
import com.cafe.ps.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false",
                "reservations.no-show-grace-minutes=20"
        }
)
class ReservationServiceIntegrationTest extends AbstractMySQLIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @BeforeEach
    void cleanDatabase() {
        reservationRepository.deleteAll();
        sessionRepository.deleteAll();
        customerRepository.deleteAll();
        pricingRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void creatingAReservationCreatesANewCustomerWhenThePhoneIsUnknown() {
        Device device = saveDevice(DeviceType.PS4);

        Reservation reservation = reservationService.create(request(
                "Ahmed", "0100000001", device.getId(), LocalDateTime.now().plusHours(2), 60
        ));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.UPCOMING);
        assertThat(reservation.getCustomer().getName()).isEqualTo("Ahmed");
        assertThat(customerRepository.findByPhone("0100000001")).isPresent();
    }

    @Test
    void creatingASecondReservationReusesTheExistingCustomerByPhone() {
        Device device1 = saveDevice(DeviceType.PS4);
        Device device2 = saveDevice(DeviceType.PS5);

        Reservation first = reservationService.create(request(
                "Ahmed", "0100000002", device1.getId(), LocalDateTime.now().plusHours(2), 60
        ));
        Reservation second = reservationService.create(request(
                "Ahmed M.", "0100000002", device2.getId(), LocalDateTime.now().plusHours(3), 60
        ));

        assertThat(second.getCustomer().getId()).isEqualTo(first.getCustomer().getId());
        assertThat(customerRepository.count()).isEqualTo(1);
    }

    @Test
    void overlappingReservationOnTheSameDeviceIsRejected() {
        Device device = saveDevice(DeviceType.PS4);
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        reservationService.create(request("Ahmed", "0100000003", device.getId(), start, 60));

        assertThatThrownBy(() -> reservationService.create(request(
                "Sara", "0100000004", device.getId(), start.plusMinutes(30), 60
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void nonOverlappingReservationOnTheSameDeviceIsAllowed() {
        Device device = saveDevice(DeviceType.PS4);
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        reservationService.create(request("Ahmed", "0100000005", device.getId(), start, 60));

        Reservation second = reservationService.create(request(
                "Sara", "0100000006", device.getId(), start.plusHours(2), 60
        ));

        assertThat(second.getStatus()).isEqualTo(ReservationStatus.UPCOMING);
    }

    @Test
    void reservationInThePastIsRejected() {
        Device device = saveDevice(DeviceType.PS4);

        assertThatThrownBy(() -> reservationService.create(request(
                "Ahmed", "0100000007", device.getId(), LocalDateTime.now().minusMinutes(5), 60
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void checkingInStartsAndLinksARealGameSession() {
        Device device = saveDevice(DeviceType.PS4);
        seedHourlyPricing(DeviceType.PS4, "40.00");
        Reservation reservation = reservationService.create(request(
                "Ahmed", "0100000008", device.getId(), LocalDateTime.now().plusHours(2), 60
        ));

        Reservation checkedIn = reservationService.checkIn(
                reservation.getId(), SessionType.SINGLE, 60, null
        );

        assertThat(checkedIn.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(checkedIn.getCheckedInAt()).isNotNull();
        assertThat(checkedIn.getGameSession()).isNotNull();
        assertThat(checkedIn.getGameSession().getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(sessionRepository.count()).isEqualTo(1);
    }

    @Test
    void checkingInTwiceFailsTheSecondTime() {
        Device device = saveDevice(DeviceType.PS4);
        seedHourlyPricing(DeviceType.PS4, "40.00");
        Reservation reservation = reservationService.create(request(
                "Ahmed", "0100000009", device.getId(), LocalDateTime.now().plusHours(2), 60
        ));
        reservationService.checkIn(reservation.getId(), SessionType.SINGLE, 60, null);

        assertThatThrownBy(() -> reservationService.checkIn(
                reservation.getId(), SessionType.SINGLE, 60, null
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("upcoming");
    }

    @Test
    void cancellingAnUpcomingReservationRecordsTheReasonAndFreesTheSlot() {
        Device device = saveDevice(DeviceType.PS4);
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        Reservation reservation = reservationService.create(request(
                "Ahmed", "0100000010", device.getId(), start, 60
        ));

        Reservation cancelled = reservationService.cancel(reservation.getId(), "Customer called to cancel");

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getCancelReason()).isEqualTo("Customer called to cancel");
        assertThat(cancelled.getCancelledAt()).isNotNull();

        // The slot is free again once cancelled.
        Reservation replacement = reservationService.create(request(
                "Sara", "0100000011", device.getId(), start, 60
        ));
        assertThat(replacement.getStatus()).isEqualTo(ReservationStatus.UPCOMING);
    }

    @Test
    void noShowSweepMarksOverdueReservationsButLeavesFutureOnesAlone() {
        Device device = saveDevice(DeviceType.PS4);
        Reservation overdue = reservationService.create(request(
                "Ahmed", "0100000012", device.getId(), LocalDateTime.now().plusMinutes(5), 30
        ));
        backdateStart(overdue, LocalDateTime.now().minusMinutes(30));

        Device otherDevice = saveDevice(DeviceType.PS5);
        Reservation notYetDue = reservationService.create(request(
                "Sara", "0100000013", otherDevice.getId(), LocalDateTime.now().plusHours(1), 60
        ));

        reservationService.markNoShows();

        assertThat(reservationRepository.findById(overdue.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.NO_SHOW);
        assertThat(reservationRepository.findById(notYetDue.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.UPCOMING);
    }

    private Device saveDevice(DeviceType type) {
        return deviceRepository.save(Device.builder()
                .name("RESERVATION-TEST-" + UUID.randomUUID())
                .type(type)
                .status(DeviceStatus.AVAILABLE)
                .active(true)
                .deleted(false)
                .build());
    }

    private void seedHourlyPricing(DeviceType deviceType, String price) {
        pricingRepository.save(Pricing.builder()
                .deviceType(deviceType)
                .sessionType(SessionType.SINGLE)
                .billingUnit(BillingUnit.HOUR)
                .price(new BigDecimal(price))
                .active(true)
                .build());
    }

    private void backdateStart(Reservation reservation, LocalDateTime startTime) {
        Reservation managed = reservationRepository.findById(reservation.getId()).orElseThrow();
        managed.setStartTime(startTime);
        reservationRepository.save(managed);
    }

    private CreateReservationRequest request(
            String name,
            String phone,
            Long deviceId,
            LocalDateTime startTime,
            int durationMinutes
    ) {
        return new CreateReservationRequest(
                name, phone, deviceId, SessionType.SINGLE, startTime, durationMinutes, null
        );
    }
}
