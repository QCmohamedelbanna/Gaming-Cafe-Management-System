package com.cafe.ps.dto;

import java.math.BigDecimal;

public record ProductSalesReport(
        Long productId,
        String productName,
        String sku,
        String unit,
        BigDecimal quantity,
        BigDecimal grossSales,
        BigDecimal discountAmount,
        BigDecimal netSales,
        BigDecimal costAmount,
        BigDecimal profit
) {
}
