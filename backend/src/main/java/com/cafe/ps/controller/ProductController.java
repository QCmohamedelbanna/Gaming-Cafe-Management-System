package com.cafe.ps.controller;

import com.cafe.ps.dto.ProductRequest;
import com.cafe.ps.dto.ProductActiveRequest;
import com.cafe.ps.entity.Product;
import com.cafe.ps.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ProductController {

    private final ProductService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'ADMIN')")
    public List<Product> getProducts() {
        return service.getActiveProducts();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<Product> getAllProducts() {
        return service.getAllProducts();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Product create(
            @Valid @RequestBody ProductRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Product update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Product setActive(
            @PathVariable Long id,
            @Valid @RequestBody ProductActiveRequest request
    ) {
        return service.setActive(id, request.active());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @PathVariable Long id
    ) {
        service.delete(id);
    }
}
