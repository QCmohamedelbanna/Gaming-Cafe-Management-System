package com.cafe.ps.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.cafe.ps.entity.BillStatus;
import com.cafe.ps.entity.PaymentMethod;

public record CheckoutResult(
        Long sessionId,
        Long orderId,
        Long billId,
        String billNumber,
        BigDecimal gamingAmount,
        BigDecimal orderAmount,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        BigDecimal amountTendered,
        BigDecimal changeAmount,
        LocalDateTime checkedOutAt,
        BillStatus status,
        List<CheckoutLine> lines
) {
}
