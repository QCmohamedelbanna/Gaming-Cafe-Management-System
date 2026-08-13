package com.cafe.ps.dto;

import com.cafe.ps.entity.SessionType;

import java.time.LocalDateTime;

public record SessionEndingSoon(
        Long sessionId,
        String deviceName,
        SessionType sessionType,
        LocalDateTime endsAt,
        long remainingSeconds
) {
}
