package com.cafe.ps.controller;

import com.cafe.ps.dto.StockMovementRequest;
import com.cafe.ps.dto.StockMovementResponse;
import com.cafe.ps.entity.Product;
import com.cafe.ps.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/products")
    public List<Product> products(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category
    ) {
        return inventoryService.getProducts(search, category);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return inventoryService.getCategories();
    }

    @GetMapping("/movements")
    public List<StockMovementResponse> movements(
            @RequestParam(required = false) Long productId
    ) {
        return inventoryService.getMovements(productId);
    }

    @PostMapping("/products/{productId}/purchase")
    public Product purchase(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request
    ) {
        return inventoryService.purchase(productId, request);
    }

    @PostMapping("/products/{productId}/adjust")
    public Product adjust(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request
    ) {
        return inventoryService.adjust(productId, request);
    }

    @PostMapping("/products/{productId}/waste")
    public Product waste(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request
    ) {
        return inventoryService.waste(productId, request);
    }
}
