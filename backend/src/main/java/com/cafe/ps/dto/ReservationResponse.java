package com.cafe.ps.dto;

import com.cafe.ps.entity.Reservation;
import com.cafe.ps.entity.ReservationStatus;
import com.cafe.ps.entity.SessionType;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        CustomerResponse customer,
        DeviceResponse device,
        SessionType sessionType,
        LocalDateTime startTime,
        Integer durationMinutes,
        ReservationStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime checkedInAt,
        LocalDateTime cancelledAt,
        String cancelReason,
        Long gameSessionId
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                CustomerResponse.from(reservation.getCustomer()),
                DeviceResponse.from(reservation.getDevice()),
                reservation.getSessionType(),
                reservation.getStartTime(),
                reservation.getDurationMinutes(),
                reservation.getStatus(),
                reservation.getNotes(),
                reservation.getCreatedAt(),
                reservation.getCheckedInAt(),
                reservation.getCancelledAt(),
                reservation.getCancelReason(),
                reservation.getGameSession() == null ? null : reservation.getGameSession().getId()
        );
    }
}
