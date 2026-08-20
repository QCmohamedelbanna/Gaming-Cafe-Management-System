package com.cafe.ps.dto;

import jakarta.validation.constraints.Size;

public record CancelReservationRequest(
        @Size(max = 200) String reason
) {
}
