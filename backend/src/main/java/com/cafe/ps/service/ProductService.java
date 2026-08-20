package com.cafe.ps.service;

import com.cafe.ps.audit.AuditLog;
import com.cafe.ps.dto.ProductRequest;
import com.cafe.ps.entity.Product;
import com.cafe.ps.entity.OrderStatus;
import com.cafe.ps.repository.OrderItemRepository;
import com.cafe.ps.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;

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
        BigDecimal sellingPrice = resolveSellingPrice(request);

        if (repository.existsByNameIgnoreCase(name)) {
            throw new IllegalStateException(
                    "A product with this name already exists"
            );
        }

        Product product = Product.builder()
                .name(name)
                .price(sellingPrice)
                .sellingPrice(sellingPrice)
                .sku(normalizeOptional(request.sku()))
                .category(normalizeCategory(request.category()))
                .costPrice(nonNegative(request.costPrice()))
                .trackStock(request.trackStock() == null || request.trackStock())
                .minimumStock(nonNegative(request.minimumStock()))
                .unit(normalizeUnit(request.unit()))
                .active(true)
                .deleted(false)
                .build();

        ensureSkuAvailable(product.getSku(), null);

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
        BigDecimal sellingPrice = resolveSellingPrice(request);

        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalStateException(
                    "A product with this name already exists"
            );
        }

        product.setName(name);
        product.setPrice(sellingPrice);
        product.setSellingPrice(sellingPrice);
        String sku = normalizeOptional(request.sku());
        ensureSkuAvailable(sku, id);
        product.setSku(sku);
        product.setCategory(normalizeCategory(request.category()));
        product.setCostPrice(nonNegative(request.costPrice()));
        if (request.trackStock() != null) product.setTrackStock(request.trackStock());
        product.setMinimumStock(nonNegative(request.minimumStock()));
        product.setUnit(normalizeUnit(request.unit()));

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
        delete(id, "unknown");
    }

    @Transactional
    public void delete(Long id, String actor) {

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
        AuditLog.record("PRODUCT_DELETE", actor, "product:" + product.getName(), "SUCCESS");
    }

    private void ensureSkuAvailable(String sku, Long id) {
        if (sku == null) return;

        boolean exists = id == null
                ? repository.existsBySkuIgnoreCase(sku)
                : repository.existsBySkuIgnoreCaseAndIdNot(sku, id);
        if (exists) {
            throw new IllegalStateException(
                    "A product with this SKU/barcode already exists"
            );
        }
    }

    private static BigDecimal resolveSellingPrice(ProductRequest request) {
        BigDecimal value = request.sellingPrice() != null
                ? request.sellingPrice()
                : request.price();
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Selling price must be greater than zero"
            );
        }
        return value;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Stock and cost values cannot be negative");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String normalizeCategory(String value) {
        return value == null || value.isBlank()
                ? "Uncategorized"
                : value.trim();
    }

    private static String normalizeUnit(String value) {
        return value == null || value.isBlank() ? "unit" : value.trim();
    }
}
