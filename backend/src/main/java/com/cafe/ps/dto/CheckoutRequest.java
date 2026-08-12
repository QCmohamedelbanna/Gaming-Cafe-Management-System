package com.cafe.ps.dto;

import com.cafe.ps.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CheckoutRequest(
        @NotNull PaymentMethod paymentMethod,
        BigDecimal amountTendered
) {
}
