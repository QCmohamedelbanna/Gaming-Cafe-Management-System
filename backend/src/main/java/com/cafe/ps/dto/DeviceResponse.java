package com.cafe.ps.dto;

import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;

public record DeviceResponse(
        Long id,
        String name,
        DeviceType type,
        DeviceStatus status,
        Boolean active,
        String maintenanceNote
) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getType(),
                device.getStatus(),
                device.getActive(),
                device.getMaintenanceNote()
        );
    }
}
