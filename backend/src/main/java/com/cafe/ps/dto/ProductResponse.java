package com.cafe.ps.dto;

import com.cafe.ps.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        BigDecimal sellingPrice,
        String sku,
        String category,
        BigDecimal costPrice,
        Boolean trackStock,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        String unit,
        Boolean active,
        Boolean lowStock
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getSellingPrice(),
                product.getSku(),
                product.getCategory(),
                product.getCostPrice(),
                product.getTrackStock(),
                product.getCurrentStock(),
                product.getMinimumStock(),
                product.getUnit(),
                product.getActive(),
                product.isLowStock()
        );
    }
}
