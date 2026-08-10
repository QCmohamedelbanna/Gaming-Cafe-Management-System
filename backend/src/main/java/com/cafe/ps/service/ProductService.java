package com.cafe.ps.service;

import com.cafe.ps.dto.ProductRequest;
import com.cafe.ps.entity.Product;
import com.cafe.ps.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    public List<Product> getActiveProducts() {
        return repository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public Product create(ProductRequest request) {

        Product product = Product.builder()
                .name(request.name().trim())
                .price(request.price())
                .active(true)
                .build();

        return repository.save(product);
    }

    @Transactional
    public Product update(
            Long id,
            ProductRequest request
    ) {

        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"
                        )
                );

        product.setName(request.name().trim());
        product.setPrice(request.price());

        return repository.save(product);
    }

    @Transactional
    public void delete(Long id) {

        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"
                        )
                );

        product.setActive(false);

        repository.save(product);
    }
}