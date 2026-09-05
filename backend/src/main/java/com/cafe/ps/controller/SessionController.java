package com.cafe.ps.controller;

import com.cafe.ps.dto.ExtendSessionRequest;
import com.cafe.ps.dto.GameSessionResponse;
import com.cafe.ps.dto.StartSessionRequest;
import com.cafe.ps.dto.CheckoutResult;
import com.cafe.ps.dto.CheckoutRequest;
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
@PreAuthorize("hasAuthority('PERMISSION_OPERATIONS_USE')")
public class SessionController {

    private final SessionService service;
    private final CheckoutService checkoutService;

    @PostMapping
    public GameSessionResponse start(
            @Valid @RequestBody StartSessionRequest req,
            Authentication authentication
    ) {
        return GameSessionResponse.from(service.start(
                req.deviceId(),
                req.sessionType(),
                req.plannedMinutes(),
                req.matchCount(),
                authentication.getName()
        ));
    }

    @PostMapping("/{id}/stop")
    public GameSessionResponse stop(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return GameSessionResponse.from(service.stop(id, authentication.getName()));
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
    public GameSessionResponse extend(
            @PathVariable Long id,
            @Valid @RequestBody ExtendSessionRequest req
    ) {
        return GameSessionResponse.from(service.extend(
                id,
                req.minutes()
        ));
    }

    @PostMapping("/{id}/match/finish")
    public GameSessionResponse finishMatch(
            @PathVariable Long id
    ) {
        return GameSessionResponse.from(service.finishCurrentMatch(id));
    }

    @PostMapping("/{id}/match/add")
    public GameSessionResponse addMatch(
            @PathVariable Long id
    ) {
        return GameSessionResponse.from(service.addMatch(id));
    }

    @GetMapping("/active")
    public List<GameSessionResponse> active() {
        return service.activeSessions().stream().map(GameSessionResponse::from).toList();
    }
}
