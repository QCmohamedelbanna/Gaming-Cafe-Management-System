package com.cafe.ps.dto;

import com.cafe.ps.entity.PaymentMethod;
import com.cafe.ps.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShiftTransactionResponse(
        Long paymentId,
        Long billId,
        String billNumber,
        PaymentMethod method,
        BigDecimal amount,
        BigDecimal amountTendered,
        BigDecimal changeAmount,
        PaymentStatus status,
        LocalDateTime paidAt,
        String cashier
) {
}
