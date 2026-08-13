package com.cafe.ps.service;

import com.cafe.ps.dto.DeviceRequest;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final GameSessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public List<Device> getAll() {
        return deviceRepository.findAll().stream()
                .peek(this::applyLegacyDefaults)
                .sorted(Comparator
                        .comparing(Device::getType)
                        .thenComparing(Device::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public Device create(DeviceRequest request) {
        String name = normalizedName(request.name());
        assertNameAvailable(name, null);

        DeviceStatus status = supportedStatus(request.status());
        return deviceRepository.save(Device.builder()
                .name(name)
                .type(request.type())
                .status(status)
                .active(true)
                .maintenanceNote(normalizedMaintenanceNote(status, request.maintenanceNote()))
                .build());
    }

    @Transactional
    public Device update(Long id, DeviceRequest request) {
        Device device = getDevice(id);
        assertNoActiveSession(device);

        String name = normalizedName(request.name());
        assertNameAvailable(name, id);

        DeviceStatus status = supportedStatus(request.status());
        device.setName(name);
        device.setType(request.type());
        device.setStatus(status);
        if (device.getActive() == null) {
            device.setActive(true);
        }
        device.setMaintenanceNote(
                normalizedMaintenanceNote(status, request.maintenanceNote())
        );

        return deviceRepository.save(device);
    }

    @Transactional
    public Device setActive(Long id, boolean active) {
        Device device = getDevice(id);
        assertNoActiveSession(device);

        device.setActive(active);
        return deviceRepository.save(device);
    }

    @Transactional
    public void delete(Long id) {
        Device device = getDevice(id);
        assertNoActiveSession(device);

        if (sessionRepository.existsByDeviceId(id)) {
            throw new IllegalStateException(
                    "This device has session history and cannot be deleted"
            );
        }

        deviceRepository.delete(device);
    }

    private Device getDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
        applyLegacyDefaults(device);
        return device;
    }

    private void assertNoActiveSession(Device device) {
        if (sessionRepository
                .findFirstByDeviceIdAndStatusOrderByStartTimeDesc(
                        device.getId(),
                        com.cafe.ps.entity.SessionStatus.ACTIVE
                )
                .isPresent()) {
            throw new IllegalStateException(
                    "This device has an active session and cannot be edited or deleted"
            );
        }
    }

    private void assertNameAvailable(String name, Long id) {
        boolean exists = id == null
                ? deviceRepository.existsByNameIgnoreCase(name)
                : deviceRepository.existsByNameIgnoreCaseAndIdNot(name, id);

        if (exists) {
            throw new IllegalStateException(
                    "A device with this name already exists"
            );
        }
    }

    private DeviceStatus supportedStatus(DeviceStatus status) {
        if (status == DeviceStatus.PLAYING || status == DeviceStatus.RESERVED) {
            throw new IllegalArgumentException(
                    "Device status must be AVAILABLE, MAINTENANCE, or OFFLINE"
            );
        }
        return status;
    }

    private String normalizedName(String name) {
        return name == null ? "" : name.trim();
    }

    private String normalizedMaintenanceNote(
            DeviceStatus status,
            String maintenanceNote
    ) {
        if (status != DeviceStatus.MAINTENANCE || maintenanceNote == null) {
            return null;
        }

        String normalized = maintenanceNote.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void applyLegacyDefaults(Device device) {
        if (device.getActive() == null) {
            // Devices that existed before the active flag was introduced are enabled.
            device.setActive(true);
        }
    }
}
