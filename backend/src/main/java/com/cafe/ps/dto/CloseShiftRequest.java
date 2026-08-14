package com.cafe.ps.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CloseShiftRequest(
        @NotNull @DecimalMin("0.00") BigDecimal actualCash,
        @Size(max = 500) String note
) {
}
