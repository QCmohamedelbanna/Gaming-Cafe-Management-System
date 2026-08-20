package com.cafe.ps.dto;

import com.cafe.ps.entity.AppSettings;
import com.cafe.ps.entity.Role;

import java.util.Set;

public record SettingsResponse(
        Boolean preventNegativeStock,
        Set<Role> discountAllowedRoles,
        Integer dashboardEndingSoonMinutes,
        Integer reservationsNoShowGraceMinutes
) {
    public static SettingsResponse from(AppSettings settings) {
        return new SettingsResponse(
                settings.getPreventNegativeStock(),
                settings.getDiscountAllowedRoles(),
                settings.getDashboardEndingSoonMinutes(),
                settings.getReservationsNoShowGraceMinutes()
        );
    }
}
