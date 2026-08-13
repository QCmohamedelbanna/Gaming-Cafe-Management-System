package com.cafe.ps.service;

import com.cafe.ps.dto.StockMovementRequest;
import com.cafe.ps.dto.StockMovementResponse;
import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.OrderItem;
import com.cafe.ps.entity.Product;
import com.cafe.ps.entity.StockMovement;
import com.cafe.ps.entity.StockMovementType;
import com.cafe.ps.repository.ProductRepository;
import com.cafe.ps.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final int STOCK_SCALE = 3;
    private static final int MONEY_SCALE = 2;

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    @Value("${inventory.prevent-negative:true}")
    private boolean preventNegativeStock;

    @Transactional(readOnly = true)
    public List<Product> getProducts(String search, String category) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        String normalizedCategory = category == null ? "" : category.trim();

        return productRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .filter(product -> normalizedSearch.isBlank()
                        || contains(product.getName(), normalizedSearch)
                        || contains(product.getSku(), normalizedSearch)
                        || contains(product.getCategory(), normalizedSearch))
                .filter(product -> normalizedCategory.isBlank()
                        || normalizedCategory.equalsIgnoreCase(product.getCategory()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .map(Product::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovements(Long productId) {
        List<StockMovement> movements = productId == null
                ? movementRepository.findTop100ByProduct_DeletedFalseOrderByCreatedAtDesc()
                : movementRepository.findTop100ByProductIdAndProduct_DeletedFalseOrderByCreatedAtDesc(productId);
        return movements.stream().map(InventoryService::toResponse).toList();
    }

    @Transactional
    public Product purchase(Long productId, StockMovementRequest request) {
        Product product = getTrackedProduct(productId);
        BigDecimal quantity = requirePositive(
                request.quantity(),
                "Purchase quantity must be greater than zero"
        );
        BigDecimal unitCost = request.unitCost() == null
                ? money(product.getCostPrice())
                : money(request.unitCost());

        // Keep the product's displayed cost aligned with the latest purchase
        // while preserving every historical unit cost in the ledger.
        product.setCostPrice(unitCost);

        applyMovement(
                product,
                StockMovementType.PURCHASE,
                quantity,
                unitCost,
                reference(request.reference(), "PURCHASE"),
                createdBy(request.createdBy())
        );
        return product;
    }

    @Transactional
    public Product adjust(Long productId, StockMovementRequest request) {
        Product product = getTrackedProduct(productId);
        BigDecimal delta = request.quantity();
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Adjustment quantity cannot be zero");
        }

        applyMovement(
                product,
                StockMovementType.ADJUSTMENT,
                stock(delta),
                request.unitCost() == null
                        ? money(product.getCostPrice())
                        : money(request.unitCost()),
                reference(request.reference(), "ADJUSTMENT"),
                createdBy(request.createdBy())
        );
        return product;
    }

    @Transactional
    public Product waste(Long productId, StockMovementRequest request) {
        Product product = getTrackedProduct(productId);
        BigDecimal quantity = requirePositive(
                request.quantity(),
                "Waste quantity must be greater than zero"
        );

        applyMovement(
                product,
                StockMovementType.WASTE,
                stock(quantity.negate()),
                request.unitCost() == null
                        ? money(product.getCostPrice())
                        : money(request.unitCost()),
                reference(request.reference(), "WASTE"),
                createdBy(request.createdBy())
        );
        return product;
    }

    /** Records stock only when a bill is actually paid. */
    public void recordSale(CafeOrder order, String reference) {
        if (order == null || order.getItems() == null) return;

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (!Boolean.TRUE.equals(product.getTrackStock())) continue;

            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
            String saleReference = reference == null ? "SALE" : reference;
            if (movementRepository.existsByProductIdAndTypeAndReference(
                    product.getId(), StockMovementType.SALE, saleReference
            )) {
                continue;
            }

            applyMovement(
                    product,
                    StockMovementType.SALE,
                    stock(quantity.negate()),
                    money(product.getCostPrice()),
                    saleReference,
                    "system"
            );
        }
    }

    /** Reverses the exact sale movements belonging to a refunded bill. */
    public void recordRefund(String saleReference, String refundReference) {
        if (saleReference == null) return;

        List<StockMovement> sales = movementRepository
                .findByTypeAndReferenceOrderByCreatedAtAsc(
                        StockMovementType.SALE,
                        saleReference
                );

        for (StockMovement sale : sales) {
            Product product = sale.getProduct();
            applyMovement(
                    product,
                    StockMovementType.RETURN,
                    stock(sale.getQuantity().abs()),
                    money(sale.getUnitCost()),
                    refundReference == null ? "RETURN" : refundReference,
                    "system"
            );
        }
    }

    private Product getTrackedProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new IllegalStateException("Deleted products cannot receive stock");
        }
        if (!Boolean.TRUE.equals(product.getTrackStock())) {
            throw new IllegalStateException(
                    "Enable stock tracking for this product before recording inventory"
            );
        }
        return product;
    }

    private void applyMovement(
            Product product,
            StockMovementType type,
            BigDecimal quantity,
            BigDecimal unitCost,
            String reference,
            String createdBy
    ) {
        BigDecimal before = stock(product.getCurrentStock());
        BigDecimal after = stock(before.add(quantity));

        if (preventNegativeStock
                && Boolean.TRUE.equals(product.getTrackStock())
                && after.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                    "Insufficient stock for " + product.getName()
                            + ". Available: " + before.stripTrailingZeros().toPlainString()
            );
        }

        product.setCurrentStock(after);
        productRepository.save(product);

        movementRepository.save(StockMovement.builder()
                .product(product)
                .type(type)
                .quantity(stock(quantity))
                .unitCost(money(unitCost))
                .reference(reference)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build());
    }

    private static StockMovementResponse toResponse(StockMovement movement) {
        Product product = movement.getProduct();
        return new StockMovementResponse(
                movement.getId(),
                product.getId(),
                product.getName(),
                movement.getType(),
                stock(movement.getQuantity()),
                money(movement.getUnitCost()),
                movement.getReference(),
                movement.getCreatedAt(),
                movement.getCreatedBy()
        );
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private static BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return stock(value);
    }

    private static String reference(String value, String prefix) {
        return value == null || value.isBlank()
                ? prefix + "-" + UUID.randomUUID()
                : value.trim();
    }

    private static String createdBy(String value) {
        return value == null || value.isBlank() ? "admin" : value.trim();
    }

    private static BigDecimal stock(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(STOCK_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
