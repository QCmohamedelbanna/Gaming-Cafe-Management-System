package com.cafe.ps.dto;

import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CafeOrderResponse(
        Long id,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        String discountReason,
        BigDecimal totalAmount,
        List<OrderItemResponse> items
) {
    public static CafeOrderResponse from(CafeOrder order) {
        return new CafeOrderResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getCompletedAt(),
                order.getStatus(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getDiscountReason(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}
