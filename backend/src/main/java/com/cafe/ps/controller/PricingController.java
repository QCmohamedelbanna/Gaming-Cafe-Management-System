package com.cafe.ps.controller;

import com.cafe.ps.dto.PricingResponse;
import com.cafe.ps.dto.UpdatePricingRequest;
import com.cafe.ps.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor

public class PricingController {

    private final PricingService pricingService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_PRICING_VIEW')")
    public List<PricingResponse> getAllPricing() {
        return pricingService.getAll().stream().map(PricingResponse::from).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_PRICING_MANAGE')")
    public PricingResponse updatePricing(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePricingRequest request
    ) {
        return PricingResponse.from(pricingService.updatePrice(
                id,
                request.price(),
                request.matchDurationMinutes(),
                request.warningBeforeExpiryMinutes()
        ));
    }
}
