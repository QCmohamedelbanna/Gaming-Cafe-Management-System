package com.cafe.ps.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdatePricingRequest(

        @DecimalMin(value = "0.01")
        BigDecimal price,

        @Min(1)
        Integer matchDurationMinutes,

        @Min(0)
        Integer warningBeforeExpiryMinutes

) {}