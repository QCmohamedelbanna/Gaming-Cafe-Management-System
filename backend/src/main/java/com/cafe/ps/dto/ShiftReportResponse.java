package com.cafe.ps.dto;

import java.math.BigDecimal;
import java.util.List;

public record ShiftReportResponse(
        ShiftResponse shift,
        BigDecimal cashCollected,
        BigDecimal cardCollected,
        BigDecimal mobileWalletCollected,
        BigDecimal totalCollected,
        BigDecimal refunds,
        BigDecimal netCollected,
        long transactionCount,
        List<ShiftTransactionResponse> transactions
) {
}
