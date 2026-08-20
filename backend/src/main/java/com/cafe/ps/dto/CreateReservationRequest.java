package com.cafe.ps.dto;

import com.cafe.ps.entity.SessionType;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateReservationRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotBlank @Size(max = 30) String customerPhone,
        @NotNull Long deviceId,
        @NotNull SessionType sessionType,
        @NotNull @Future LocalDateTime startTime,
        @NotNull @Min(15) Integer durationMinutes,
        @Size(max = 300) String notes
) {
}
