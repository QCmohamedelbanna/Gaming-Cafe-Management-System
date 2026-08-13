package com.cafe.ps.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank
        String name,

        @DecimalMin("0.01")
        BigDecimal price,

        @DecimalMin("0.01")
        BigDecimal sellingPrice,

        String sku,
        String category,

        @DecimalMin("0.00")
        BigDecimal costPrice,

        Boolean trackStock,

        @DecimalMin("0.00")
        BigDecimal minimumStock,

        String unit

) {
}
