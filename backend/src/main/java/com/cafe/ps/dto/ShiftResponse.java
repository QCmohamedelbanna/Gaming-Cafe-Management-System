package com.cafe.ps.dto;

import com.cafe.ps.entity.Role;
import com.cafe.ps.entity.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShiftResponse(
        Long id,
        Long userId,
        String username,
        String displayName,
        Role role,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        ShiftStatus status,
        BigDecimal openingCash,
        BigDecimal expectedCash,
        BigDecimal actualCash,
        BigDecimal cashDifference,
        String closingNote
) {
}
