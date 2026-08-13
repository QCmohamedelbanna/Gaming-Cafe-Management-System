package com.cafe.ps.service;

import com.cafe.ps.entity.*;
import com.cafe.ps.repository.*;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CafeOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final GameSessionRepository sessionRepository;

    @Value("${inventory.prevent-negative:true}")
    private boolean preventNegativeStock;

    @Transactional
    public CafeOrder createOrder(Long gameSessionId) {

        GameSession session = null;

        if (gameSessionId != null) {

            session = sessionRepository.findById(gameSessionId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Game session not found"
                            )
                    );

            if (session.getStatus() != SessionStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Products can only be added to an active session"
                );
            }

            var existing = orderRepository
                    .findFirstByGameSessionIdAndStatusOrderByCreatedAtDesc(
                            gameSessionId,
                            OrderStatus.OPEN
                    );

            if (existing.isPresent()) {
                return existing.get();
            }
        }

        return orderRepository.save(
                CafeOrder.builder()
                        .gameSession(session)
                        .createdAt(LocalDateTime.now())
                        .status(OrderStatus.OPEN)
                        .totalAmount(BigDecimal.ZERO)
                        .build()
        );
    }

    @Transactional
    public CafeOrder addItem(
            Long orderId,
            Long productId,
            Integer quantity
    ) {

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least one");
        }

        CafeOrder order = getOpenOrder(orderId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"
                        )
                );

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalStateException(
                    "Product is disabled"
            );
        }

        /*
         * لو المنتج موجود بالفعل في الطلب،
         * نزود الكمية بدل إنشاء row جديد.
         */
        OrderItem existingItem = order.getItems()
                .stream()
                .filter(item ->
                        item.getProduct()
                                .getId()
                                .equals(productId)
                )
                .findFirst()
                .orElse(null);

        int currentQuantity = existingItem == null
                ? 0
                : existingItem.getQuantity();
        int requestedQuantity = currentQuantity + quantity;

        ensureStockAvailable(product, requestedQuantity);

        if (existingItem != null) {

            existingItem.setQuantity(requestedQuantity);

            existingItem.setLineTotal(
                    money(existingItem
                            .getUnitPriceSnapshot()
                            .multiply(
                                    BigDecimal.valueOf(
                                            requestedQuantity
                                    )
                            ))
            );

        } else {

            BigDecimal lineTotal =
                    money(product.effectiveSellingPrice()
                            .multiply(
                                    BigDecimal.valueOf(quantity)
                            ));

            OrderItem item =
                    OrderItem.builder()
                            .order(order)
                            .product(product)
                            .quantity(quantity)
                            .unitPriceSnapshot(
                                    product.effectiveSellingPrice()
                            )
                            .lineTotal(lineTotal)
                            .build();

            order.addItem(item);
        }

        recalculateTotal(order);

        return orderRepository.save(order);
    }

    private void ensureStockAvailable(Product product, int requestedQuantity) {
        if (!preventNegativeStock
                || !Boolean.TRUE.equals(product.getTrackStock())) {
            return;
        }

        BigDecimal available = product.getCurrentStock() == null
                ? BigDecimal.ZERO
                : product.getCurrentStock();

        if (BigDecimal.valueOf(requestedQuantity).compareTo(available) > 0) {
            throw new IllegalStateException(
                    "Insufficient stock for " + product.getName()
                            + ". Available: "
                            + available.stripTrailingZeros().toPlainString()
            );
        }
    }

    @Transactional
    public CafeOrder removeItem(
            Long orderId,
            Long itemId
    ) {

        CafeOrder order = getOpenOrder(orderId);

        boolean removed = order.getItems()
                .removeIf(item ->
                        item.getId().equals(itemId)
                );

        if (!removed) {
            throw new IllegalArgumentException(
                    "Order item not found"
            );
        }

        recalculateTotal(order);

        if (order.getItems().isEmpty()
                && order.getGameSession() == null) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        return orderRepository.save(order);
    }

    public CafeOrder get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order not found"
                        )
                );
    }

    private CafeOrder getOpenOrder(Long orderId) {

        CafeOrder order = get(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new IllegalStateException(
                    "Order is not open"
            );
        }

        return order;
    }

    private void recalculateTotal(CafeOrder order) {

        BigDecimal total = order.getItems()
                .stream()
                .map(item -> {
                    BigDecimal line = money(item.getLineTotal());
                    item.setLineTotal(line);
                    return line;
                })
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        order.setTotalAmount(money(total));
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public CafeOrder getOpenOrderForSession(Long sessionId) {

        return orderRepository
                .findFirstByGameSessionIdAndStatusOrderByCreatedAtDesc(
                        sessionId,
                        OrderStatus.OPEN
                )
                .orElse(null);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelStaleEmptyStandaloneOrders() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(5);
        List<CafeOrder> stale = orderRepository.findStaleEmptyStandaloneOrders(
                OrderStatus.OPEN,
                before
        );

        stale.stream()
                .filter(order -> order.getItems().isEmpty())
                .forEach(order -> order.setStatus(OrderStatus.CANCELLED));

        if (!stale.isEmpty()) orderRepository.saveAll(stale);
    }
}
