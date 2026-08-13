package com.cafe.ps.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockMovementRequest(

        @NotNull
        BigDecimal quantity,

        @DecimalMin("0.00")
        BigDecimal unitCost,

        String reference,

        String createdBy
) {
}
