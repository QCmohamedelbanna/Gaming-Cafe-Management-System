package com.cafe.ps.dto;

import com.cafe.ps.entity.DeviceControlProvider;
import com.cafe.ps.entity.DeviceShutdownPolicy;
import jakarta.validation.constraints.Size;

public record DeviceControlConfigurationRequest(
        DeviceControlProvider provider,
        @Size(max = 255) String controllerDeviceId,
        @Size(max = 100) String controllerPowerCode,
        Boolean enabled,
        DeviceShutdownPolicy shutdownPolicy
) {
}
