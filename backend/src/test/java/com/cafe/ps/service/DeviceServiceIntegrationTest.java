package com.cafe.ps.service;

import com.cafe.ps.dto.DeviceRequest;
import com.cafe.ps.entity.BillingUnit;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:device-tests?mode=memory&cache=shared",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false",
                "spring.datasource.hikari.maximum-pool-size=1"
        }
)
class DeviceServiceIntegrationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @BeforeEach
    void cleanDatabase() {
        sessionRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void createAndUpdateDeviceStoresTypeStatusAndMaintenanceNote() {
        Device created = deviceService.create(new DeviceRequest(
                "PS5-Lounge",
                DeviceType.PS5,
                DeviceStatus.MAINTENANCE,
                "HDMI cable replacement"
        ));

        assertThat(created.getActive()).isTrue();
        assertThat(created.getType()).isEqualTo(DeviceType.PS5);
        assertThat(created.getStatus()).isEqualTo(DeviceStatus.MAINTENANCE);
        assertThat(created.getMaintenanceNote()).isEqualTo("HDMI cable replacement");

        Device updated = deviceService.update(created.getId(), new DeviceRequest(
                "PS5-Lounge-2",
                DeviceType.PS4,
                DeviceStatus.OFFLINE,
                "Awaiting replacement controller"
        ));

        assertThat(updated.getName()).isEqualTo("PS5-Lounge-2");
        assertThat(updated.getType()).isEqualTo(DeviceType.PS4);
        assertThat(updated.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
        assertThat(updated.getMaintenanceNote()).isNull();
    }

    @Test
    void activationIsIndependentFromOperationalStatus() {
        Device device = deviceService.create(new DeviceRequest(
                "PS4-Maintenance",
                DeviceType.PS4,
                DeviceStatus.MAINTENANCE,
                "Fan cleaning"
        ));

        Device deactivated = deviceService.setActive(device.getId(), false);
        Device activated = deviceService.setActive(device.getId(), true);

        assertThat(deactivated.getActive()).isFalse();
        assertThat(activated.getActive()).isTrue();
        assertThat(activated.getStatus()).isEqualTo(DeviceStatus.MAINTENANCE);
    }

    @Test
    void duplicateNamesAreRejected() {
        deviceService.create(new DeviceRequest(
                "PS4-Front",
                DeviceType.PS4,
                DeviceStatus.AVAILABLE,
                null
        ));

        assertThatThrownBy(() -> deviceService.create(new DeviceRequest(
                "  ps4-front ",
                DeviceType.PS5,
                DeviceStatus.AVAILABLE,
                null
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A device with this name already exists");
    }

    @Test
    void activeSessionProtectsDeviceFromEditActivationAndDelete() {
        Device device = saveDevice("PS4-Busy", DeviceType.PS4, DeviceStatus.PLAYING);
        saveActiveSession(device);

        assertThatThrownBy(() -> deviceService.update(device.getId(), new DeviceRequest(
                "PS4-Renamed",
                DeviceType.PS4,
                DeviceStatus.AVAILABLE,
                null
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("This device has an active session and cannot be edited or deleted");

        assertThatThrownBy(() -> deviceService.setActive(device.getId(), false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("This device has an active session and cannot be edited or deleted");

        assertThatThrownBy(() -> deviceService.delete(device.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("This device has an active session and cannot be edited or deleted");

        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getName())
                .isEqualTo("PS4-Busy");
    }

    @Test
    void unusedDeviceCanBeSoftDeletedAndDeviceHistoryIsPreserved() {
        Device unused = saveDevice("PS4-Unused", DeviceType.PS4, DeviceStatus.OFFLINE);
        deviceService.delete(unused.getId());
        assertThat(deviceRepository.findById(unused.getId()).orElseThrow().getDeleted())
                .isTrue();
        assertThat(deviceService.getAll())
                .noneMatch(device -> device.getId().equals(unused.getId()));

        Device withHistory = saveDevice("PS4-History", DeviceType.PS4, DeviceStatus.AVAILABLE);
        GameSession completed = saveSession(withHistory, SessionStatus.COMPLETED);

        deviceService.delete(withHistory.getId());

        Device deletedWithHistory = deviceRepository.findById(withHistory.getId()).orElseThrow();
        assertThat(deletedWithHistory.getDeleted()).isTrue();
        assertThat(deletedWithHistory.getActive()).isFalse();
        assertThat(sessionRepository.existsById(completed.getId())).isTrue();
        assertThat(deviceService.getAll())
                .noneMatch(device -> device.getId().equals(withHistory.getId()));
    }

    @Test
    void onlySupportedAdministrativeStatusesCanBeSaved() {
        assertThatThrownBy(() -> deviceService.create(new DeviceRequest(
                "PS4-Playing",
                DeviceType.PS4,
                DeviceStatus.PLAYING,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Device status must be AVAILABLE, MAINTENANCE, or OFFLINE");
    }

    private Device saveDevice(String name, DeviceType type, DeviceStatus status) {
        return deviceRepository.save(Device.builder()
                .name(name)
                .type(type)
                .status(status)
                .active(true)
                .build());
    }

    private GameSession saveActiveSession(Device device) {
        return saveSession(device, SessionStatus.ACTIVE);
    }

    private GameSession saveSession(Device device, SessionStatus status) {
        return sessionRepository.save(GameSession.builder()
                .device(device)
                .startTime(START)
                .plannedMinutes(60)
                .hourlyRateSnapshot(new BigDecimal("50.00"))
                .sessionType(SessionType.SINGLE)
                .billingUnit(BillingUnit.HOUR)
                .unitPriceSnapshot(new BigDecimal("50.00"))
                .status(status)
                .build());
    }
}
