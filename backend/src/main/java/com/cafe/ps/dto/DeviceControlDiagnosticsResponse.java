package com.cafe.ps.dto;

import com.cafe.ps.entity.DeviceControlProvider;
import com.cafe.ps.entity.DevicePowerState;

import java.time.Instant;
import java.util.List;

public record DeviceControlDiagnosticsResponse(
        Long deviceId,
        String deviceName,
        DeviceControlProvider provider,
        String maskedControllerDeviceId,
        DevicePowerState physicalState,
        List<String> powerCommandCodes,
        boolean success,
        String message,
        Instant timestamp
) {
}
