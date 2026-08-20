package com.cafe.ps.dto;

import com.cafe.ps.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        ProductResponse product,
        Integer quantity,
        BigDecimal unitPriceSnapshot,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct() == null ? null : ProductResponse.from(item.getProduct()),
                item.getQuantity(),
                item.getUnitPriceSnapshot(),
                item.getLineTotal()
        );
    }
}
