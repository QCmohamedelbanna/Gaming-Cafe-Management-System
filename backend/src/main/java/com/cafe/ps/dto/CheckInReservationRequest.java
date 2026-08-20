package com.cafe.ps.dto;

import com.cafe.ps.entity.SessionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckInReservationRequest(
        @NotNull SessionType sessionType,
        @Min(1) Integer plannedMinutes,
        @Min(1) Integer matchCount
) {
}
