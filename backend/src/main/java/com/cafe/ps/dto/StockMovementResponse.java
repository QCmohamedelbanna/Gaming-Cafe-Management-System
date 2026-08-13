package com.cafe.ps.dto;

import com.cafe.ps.entity.StockMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productName,
        StockMovementType type,
        BigDecimal quantity,
        BigDecimal unitCost,
        String reference,
        LocalDateTime createdAt,
        String createdBy
) {
}
