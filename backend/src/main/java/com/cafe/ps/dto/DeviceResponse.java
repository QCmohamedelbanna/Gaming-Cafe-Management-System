package com.cafe.ps.dto;

import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.DeviceControlProvider;
import com.cafe.ps.entity.DevicePowerState;
import com.cafe.ps.entity.DeviceShutdownPolicy;

import java.time.LocalDateTime;

public record DeviceResponse(
        Long id,
        String name,
        DeviceType type,
        DeviceStatus status,
        Boolean active,
        String maintenanceNote,
        DeviceControlProvider controlProvider,
        String controllerDeviceId,
        String controllerPowerCode,
        Boolean powerControlEnabled,
        DevicePowerState physicalPowerStatus,
        LocalDateTime lastControlAt,
        String lastControlError,
        DeviceShutdownPolicy shutdownPolicy
) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getType(),
                device.getStatus(),
                device.getActive(),
                device.getMaintenanceNote(),
                device.getControlProvider(),
                device.getControllerDeviceId(),
                device.getControllerPowerCode(),
                device.getPowerControlEnabled(),
                device.getPhysicalPowerStatus(),
                device.getLastControlAt(),
                device.getLastControlError(),
                device.getShutdownPolicy()
        );
    }
}
