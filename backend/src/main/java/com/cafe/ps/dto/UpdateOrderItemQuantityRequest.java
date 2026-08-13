package com.cafe.ps.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderItemQuantityRequest(
        @NotNull @Min(0) Integer quantity
) {
}
