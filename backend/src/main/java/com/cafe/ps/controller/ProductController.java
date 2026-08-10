package com.cafe.ps.controller;

import com.cafe.ps.dto.ProductRequest;
import com.cafe.ps.entity.Product;
import com.cafe.ps.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductService service;

    @GetMapping
    public List<Product> getProducts() {
        return service.getActiveProducts();
    }

    @PostMapping
    public Product create(
            @Valid @RequestBody ProductRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Product update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {
        service.delete(id);
    }
}