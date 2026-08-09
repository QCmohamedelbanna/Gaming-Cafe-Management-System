package com.cafe.ps.controller;

import com.cafe.ps.dto.UpdatePricingRequest;
import com.cafe.ps.entity.Pricing;
import com.cafe.ps.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")

public class PricingController {

    private final PricingService pricingService;

    @GetMapping
    public List<Pricing> getAllPricing() {
        return pricingService.getAll();
    }

    @PutMapping("/{id}")
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