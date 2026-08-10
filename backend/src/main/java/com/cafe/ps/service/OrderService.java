package com.cafe.ps.service;

import com.cafe.ps.entity.*;
import com.cafe.ps.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CafeOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final GameSessionRepository sessionRepository;

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

        if (existingItem != null) {

            int newQuantity =
                    existingItem.getQuantity() + quantity;

            existingItem.setQuantity(newQuantity);

            existingItem.setLineTotal(
                    existingItem
                            .getUnitPriceSnapshot()
                            .multiply(
                                    BigDecimal.valueOf(
                                            newQuantity
                                    )
                            )
            );

        } else {

            BigDecimal lineTotal =
                    product.getPrice()
                            .multiply(
                                    BigDecimal.valueOf(quantity)
                            );

            OrderItem item =
                    OrderItem.builder()
                            .order(order)
                            .product(product)
                            .quantity(quantity)
                            .unitPriceSnapshot(
                                    product.getPrice()
                            )
                            .lineTotal(lineTotal)
                            .build();

            order.addItem(item);
        }

        recalculateTotal(order);

        return orderRepository.save(order);
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

        return orderRepository.save(order);
    }

    @Transactional
    public CafeOrder completeOrder(Long orderId) {

        CafeOrder order = getOpenOrder(orderId);

        recalculateTotal(order);

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

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
                .map(OrderItem::getLineTotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        order.setTotalAmount(total);
    }
}