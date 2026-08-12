package com.cafe.ps.dto;

import jakarta.validation.constraints.NotNull;

public record ProductActiveRequest(
        @NotNull Boolean active
) {
}
