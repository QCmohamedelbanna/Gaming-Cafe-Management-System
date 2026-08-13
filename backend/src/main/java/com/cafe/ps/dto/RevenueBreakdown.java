package com.cafe.ps.dto;

import java.math.BigDecimal;

public record RevenueBreakdown(
        String label,
        BigDecimal gamingRevenue,
        BigDecimal cafeRevenue,
        BigDecimal totalRevenue,
        long billCount
) {
}
