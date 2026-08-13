package com.cafe.ps.dto;

import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull DeviceType type,
        @NotNull DeviceStatus status,
        @Size(max = 500) String maintenanceNote
) {
}
