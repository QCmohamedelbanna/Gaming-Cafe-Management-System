package com.cafe.ps.dto;

import com.cafe.ps.entity.Role;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateSettingsRequest(
        @NotNull Boolean preventNegativeStock,
        @NotNull Set<Role> discountAllowedRoles,
        @NotNull @Min(1) Integer dashboardEndingSoonMinutes,
        @NotNull @Min(1) Integer reservationsNoShowGraceMinutes
) {
}
