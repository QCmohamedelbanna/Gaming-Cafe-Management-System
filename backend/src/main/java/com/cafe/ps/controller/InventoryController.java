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
    @PreAuthorize("hasAuthority('PERMISSION_INVENTORY_VIEW')")
    public List<Product> products(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category
    ) {
        return inventoryService.getProducts(search, category);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('PERMISSION_INVENTORY_VIEW')")
    public List<String> categories() {
        return inventoryService.getCategories();
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('PERMISSION_INVENTORY_VIEW')")
    public List<StockMovementResponse> movements(
            @RequestParam(required = false) Long productId
    ) {
        return inventoryService.getMovements(productId);
    }

    @PostMapping("/products/{productId}/purchase")
    @PreAuthorize("hasAuthority('PERMISSION_INVENTORY_MANAGE')")
    public Product purchase(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request,
            Authentication authentication
    ) {
        return inventoryService.purchase(productId, withActor(request, authentication));
    }

    @PostMapping("/products/{productId}/adjust")
    @PreAuthorize("hasAuthority('PERMISSION_INVENTORY_MANAGE')")
    public Product adjust(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request,
            Authentication authentication
    ) {
        return inventoryService.adjust(productId, withActor(request, authentication));
    }

    @PostMapping("/products/{productId}/waste")
    @PreAuthorize("hasAuthority('PERMISSION_INVENTORY_MANAGE')")
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
