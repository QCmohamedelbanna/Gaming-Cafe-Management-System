package com.cafe.ps.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddOrderItemRequest(

        @NotNull
        Long productId,

        @NotNull
        @Min(1)
        Integer quantity

) {
}