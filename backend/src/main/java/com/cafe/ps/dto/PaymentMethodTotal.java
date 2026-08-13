package com.cafe.ps.dto;

import com.cafe.ps.entity.PaymentMethod;

import java.math.BigDecimal;

public record PaymentMethodTotal(
        PaymentMethod method,
        BigDecimal amount,
        long transactionCount
) {
}
