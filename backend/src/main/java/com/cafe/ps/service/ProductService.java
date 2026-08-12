package com.cafe.ps.service;

import com.cafe.ps.dto.ProductRequest;
import com.cafe.ps.entity.Product;
import com.cafe.ps.entity.OrderStatus;
import com.cafe.ps.repository.OrderItemRepository;
import com.cafe.ps.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final OrderItemRepository orderItemRepository;

    public List<Product> getActiveProducts() {
        return repository
                .findAllByActiveTrueAndDeletedFalseOrderByNameAsc();
    }

    public List<Product> getAllProducts() {
        return repository.findAllByDeletedFalseOrderByNameAsc();
    }

    @Transactional
    public Product create(ProductRequest request) {

        String name = request.name().trim();

        if (repository.existsByNameIgnoreCase(name)) {
            throw new IllegalStateException(
                    "A product with this name already exists"
            );
        }

        Product product = Product.builder()
                .name(name)
                .price(request.price())
                .active(true)
                .deleted(false)
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

        String name = request.name().trim();

        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalStateException(
                    "A product with this name already exists"
            );
        }

        product.setName(name);
        product.setPrice(request.price());

        return repository.save(product);
    }

    @Transactional
    public Product setActive(Long id, boolean active) {

        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"
                        )
                );

        product.setActive(active);

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

        if (orderItemRepository.existsByProductIdAndOrder_Status(
                id,
                OrderStatus.OPEN
        )) {
            throw new IllegalStateException(
                    "This product is in an open order. Complete checkout before deleting it."
            );
        }

        product.setActive(false);
        product.setDeleted(true);
        repository.save(product);
    }
}
