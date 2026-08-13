package com.cafe.ps.dto;

import jakarta.validation.constraints.NotNull;

public record DeviceActiveRequest(
        @NotNull Boolean active
) {
}
