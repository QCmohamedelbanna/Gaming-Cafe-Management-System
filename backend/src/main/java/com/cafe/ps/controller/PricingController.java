package com.cafe.ps.controller;

import com.cafe.ps.dto.UpdatePricingRequest;
import com.cafe.ps.entity.Pricing;
import com.cafe.ps.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

public class PricingController {

    private final PricingService pricingService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_PRICING_VIEW')")
    public List<Pricing> getAllPricing() {
        return pricingService.getAll();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_PRICING_MANAGE')")
    public Pricing updatePricing(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePricingRequest request
    ) {
        return pricingService.updatePrice(
                id,
                request.price(),
                request.matchDurationMinutes(),
                request.warningBeforeExpiryMinutes()
        );
    }
}
