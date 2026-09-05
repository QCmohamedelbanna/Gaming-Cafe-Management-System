package com.cafe.ps.dto;

import com.cafe.ps.entity.DeviceControlProvider;
import com.cafe.ps.entity.DevicePowerState;

import java.time.Instant;

public record PowerCommandResult(
        boolean success,
        DeviceControlProvider provider,
        DevicePowerState physicalState,
        String message,
        Instant timestamp
) {
    public static PowerCommandResult success(
            DeviceControlProvider provider,
            DevicePowerState state,
            String message
    ) {
        return new PowerCommandResult(true, provider, state, message, Instant.now());
    }

    public static PowerCommandResult failure(
            DeviceControlProvider provider,
            DevicePowerState state,
            String message
    ) {
        return new PowerCommandResult(false, provider, state, message, Instant.now());
    }
}
