package com.cafe.ps.dto;

import java.math.BigDecimal;

public record CheckoutLine(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
