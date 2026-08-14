package com.cafe.ps.controller;

import com.cafe.ps.dto.StockMovementRequest;
import com.cafe.ps.dto.StockMovementResponse;
import com.cafe.ps.entity.Product;
import com.cafe.ps.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'ADMIN')")
    public List<Product> products(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category
    ) {
        return inventoryService.getProducts(search, category);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'ADMIN')")
    public List<String> categories() {
        return inventoryService.getCategories();
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'ADMIN')")
    public List<StockMovementResponse> movements(
            @RequestParam(required = false) Long productId
    ) {
        return inventoryService.getMovements(productId);
    }

    @PostMapping("/products/{productId}/purchase")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Product purchase(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request,
            Authentication authentication
    ) {
        return inventoryService.purchase(productId, withActor(request, authentication));
    }

    @PostMapping("/products/{productId}/adjust")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Product adjust(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request,
            Authentication authentication
    ) {
        return inventoryService.adjust(productId, withActor(request, authentication));
    }

    @PostMapping("/products/{productId}/waste")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Product waste(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request,
            Authentication authentication
    ) {
        return inventoryService.waste(productId, withActor(request, authentication));
    }

    private static StockMovementRequest withActor(
            StockMovementRequest request,
            Authentication authentication
    ) {
        return new StockMovementRequest(
                request.quantity(),
                request.unitCost(),
                request.reference(),
                authentication.getName()
        );
    }
}
