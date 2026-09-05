package com.cafe.ps.service;

import com.cafe.ps.dto.PowerCommandResult;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DevicePowerState;
import com.cafe.ps.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Persists physical telemetry independently from financial transactions. */
@Service
@RequiredArgsConstructor
public class DeviceControlStateService {

    private final DeviceRepository deviceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long deviceId, PowerCommandResult result) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) return;

        device.setPhysicalPowerStatus(result.physicalState() == null
                ? DevicePowerState.UNKNOWN
                : result.physicalState());
        device.setLastControlAt(LocalDateTime.now());
        device.setLastControlError(result.success() ? null : truncate(result.message()));
        deviceRepository.save(device);
    }

    private static String truncate(String message) {
        if (message == null || message.isBlank()) return "Hardware control failed";
        String sanitized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500);
    }
}
