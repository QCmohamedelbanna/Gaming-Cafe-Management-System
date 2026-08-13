package com.cafe.ps.dto;

import java.math.BigDecimal;

public record CashierShiftSummary(
        String cashier,
        BigDecimal totalCollected,
        BigDecimal cashCollected,
        BigDecimal cardCollected,
        BigDecimal mobileWalletCollected,
        BigDecimal refunds,
        BigDecimal netCollected,
        long paymentCount
) {
}
