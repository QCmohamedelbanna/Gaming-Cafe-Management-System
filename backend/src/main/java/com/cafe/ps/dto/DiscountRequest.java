package com.cafe.ps.dto;

import com.cafe.ps.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DiscountRequest(
        @NotNull DiscountType type,
        @NotNull @DecimalMin("0.00") BigDecimal value,
        @Size(max = 200) String reason
) {
}
