package com.cafe.ps.controller;

import com.cafe.ps.dto.ProductRequest;
import com.cafe.ps.dto.ProductActiveRequest;
import com.cafe.ps.dto.ProductResponse;
import com.cafe.ps.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_PRODUCTS_VIEW')")
    public List<ProductResponse> getProducts() {
        return service.getActiveProducts().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('PERMISSION_PRODUCTS_MANAGE')")
    public List<ProductResponse> getAllProducts() {
        return service.getAllProducts().stream().map(ProductResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_PRODUCTS_MANAGE')")
    public ProductResponse create(
            @Valid @RequestBody ProductRequest request
    ) {
        return ProductResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_PRODUCTS_MANAGE')")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ProductResponse.from(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('PERMISSION_PRODUCTS_MANAGE')")
    public ProductResponse setActive(
            @PathVariable Long id,
            @Valid @RequestBody ProductActiveRequest request
    ) {
        return ProductResponse.from(service.setActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_PRODUCTS_MANAGE') and hasAuthority('PERMISSION_DESTRUCTIVE_OPERATIONS')")
    public void delete(
            @PathVariable Long id
    ) {
        service.delete(id);
    }
}
