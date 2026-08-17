package com.cafe.ps.controller;

import com.cafe.ps.dto.ExtendSessionRequest;
import com.cafe.ps.dto.StartSessionRequest;
import com.cafe.ps.dto.CheckoutResult;
import com.cafe.ps.dto.CheckoutRequest;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.service.CheckoutService;
import com.cafe.ps.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@PreAuthorize("hasAuthority('PERMISSION_OPERATIONS_USE')")
public class SessionController {

    private final SessionService service;
    private final CheckoutService checkoutService;

    @PostMapping
    public GameSession start(
            @Valid @RequestBody StartSessionRequest req
    ) {
        return service.start(
                req.deviceId(),
                req.sessionType(),
                req.plannedMinutes(),
                req.matchCount()
        );
    }

    @PostMapping("/{id}/stop")
    public GameSession stop(
            @PathVariable Long id
    ) {
        return service.stop(id);
    }

    @PostMapping("/{id}/checkout/prepare")
    @PreAuthorize("hasAuthority('PERMISSION_CHECKOUT_USE')")
    public CheckoutResult prepareCheckout(
            @PathVariable Long id
    ) {
        return checkoutService.prepareCheckout(id);
    }

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAuthority('PERMISSION_CHECKOUT_USE')")
    public CheckoutResult checkout(
            @PathVariable Long id,
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication
    ) {
        return checkoutService.checkout(
                id,
                request.paymentMethod(),
                request.amountTendered(),
                authentication.getName()
        );
    }

    @PostMapping("/{id}/extend")
    public GameSession extend(
            @PathVariable Long id,
            @Valid @RequestBody ExtendSessionRequest req
    ) {
        return service.extend(
                id,
                req.minutes()
        );
    }

    @PostMapping("/{id}/match/finish")
    public GameSession finishMatch(
            @PathVariable Long id
    ) {
        return service.finishCurrentMatch(id);
    }

    @PostMapping("/{id}/match/add")
    public GameSession addMatch(
            @PathVariable Long id
    ) {
        return service.addMatch(id);
    }

    @GetMapping("/active")
    public List<GameSession> active() {
        return service.activeSessions();
    }
}
