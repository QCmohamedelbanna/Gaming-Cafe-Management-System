package com.cafe.ps.dto;

import com.cafe.ps.entity.DeviceControlProvider;
import com.cafe.ps.entity.DevicePowerState;

import java.time.Instant;

public record DevicePowerStatusResponse(
        Long deviceId,
        String deviceName,
        DeviceControlProvider provider,
        DevicePowerState physicalState,
        boolean success,
        String message,
        Instant timestamp
) {
}
