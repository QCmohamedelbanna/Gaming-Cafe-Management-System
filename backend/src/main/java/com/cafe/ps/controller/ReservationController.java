package com.cafe.ps.controller;

import com.cafe.ps.dto.CancelReservationRequest;
import com.cafe.ps.dto.CheckInReservationRequest;
import com.cafe.ps.dto.CreateReservationRequest;
import com.cafe.ps.dto.ReservationResponse;
import com.cafe.ps.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERMISSION_RESERVATIONS_MANAGE')")
public class ReservationController {

    private final ReservationService service;

    @GetMapping
    public List<ReservationResponse> list(
            @RequestParam(required = false, defaultValue = "false") boolean all
    ) {
        return (all ? service.all() : service.upcoming())
                .stream().map(ReservationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id) {
        return ReservationResponse.from(service.get(id));
    }

    @PostMapping
    public ReservationResponse create(@Valid @RequestBody CreateReservationRequest request) {
        return ReservationResponse.from(service.create(request));
    }

    @PostMapping("/{id}/check-in")
    public ReservationResponse checkIn(
            @PathVariable Long id,
            @Valid @RequestBody CheckInReservationRequest request
    ) {
        return ReservationResponse.from(service.checkIn(
                id,
                request.sessionType(),
                request.plannedMinutes(),
                request.matchCount()
        ));
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(
            @PathVariable Long id,
            @RequestBody(required = false) CancelReservationRequest request
    ) {
        return ReservationResponse.from(service.cancel(id, request == null ? null : request.reason()));
    }
}
