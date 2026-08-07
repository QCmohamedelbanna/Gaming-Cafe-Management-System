package com.cafe.ps.dto;
import com.cafe.ps.entity.SessionType;
import jakarta.validation.constraints.*;
public record StartSessionRequest(
        @NotNull Long deviceId,
        @NotNull SessionType sessionType,
        @Min(1) Integer plannedMinutes,
        @Min(1) Integer matchCount
) {}
