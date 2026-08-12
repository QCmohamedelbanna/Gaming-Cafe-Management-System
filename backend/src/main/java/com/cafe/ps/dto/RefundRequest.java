package com.cafe.ps.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(
        @NotBlank String reason
) {
}
