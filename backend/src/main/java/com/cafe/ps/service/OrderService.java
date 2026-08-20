package com.cafe.ps.service;

import com.cafe.ps.audit.AuditLog;
import com.cafe.ps.dto.DiscountRequest;
import com.cafe.ps.entity.*;
import com.cafe.ps.repository.*;
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
    private final SettingsService settingsService;

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
        if (!Boolean.TRUE.equals(settingsService.get().getPreventNegativeStock())
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

        cancelEmptyStandaloneOrder(order);

        return orderRepository.save(order);
    }

    @Transactional
    public CafeOrder updateItemQuantity(
            Long orderId,
            Long itemId,
            Integer quantity
    ) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        CafeOrder order = getOpenOrder(orderId);
        OrderItem item = order.getItems().stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order item not found"));

        if (quantity == 0) {
            order.getItems().remove(item);
            recalculateTotal(order);
            cancelEmptyStandaloneOrder(order);
            return orderRepository.save(order);
        }

        ensureStockAvailable(item.getProduct(), quantity);
        item.setQuantity(quantity);
        item.setLineTotal(money(item.getUnitPriceSnapshot()
                .multiply(BigDecimal.valueOf(quantity))));
        recalculateTotal(order);
        return orderRepository.save(order);
    }

    @Transactional
    public CafeOrder applyDiscount(
            Long orderId,
            DiscountRequest request,
            String userRole
    ) {
        return applyDiscount(orderId, request, hasDiscountPermission(userRole), userRole);
    }

    @Transactional
    public CafeOrder applyDiscount(
            Long orderId,
            DiscountRequest request,
            boolean hasDiscountPermission
    ) {
        return applyDiscount(orderId, request, hasDiscountPermission, "unknown");
    }

    @Transactional
    public CafeOrder applyDiscount(
            Long orderId,
            DiscountRequest request,
            boolean hasDiscountPermission,
            String actor
    ) {
        CafeOrder order = getOpenOrder(orderId);
        BigDecimal subtotal = calculateSubtotal(order);
        BigDecimal value = money(request.value());

        if (value.compareTo(BigDecimal.ZERO) > 0
                && !hasDiscountPermission) {
            throw new IllegalStateException(
                    "Discounts require manager or administrator permission"
            );
        }

        BigDecimal discount;
        if (request.type() == DiscountType.PERCENTAGE) {
            if (value.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
            discount = money(subtotal.multiply(value)
                    .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        } else {
            discount = value;
        }

        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "Discount cannot exceed the order subtotal"
            );
        }

        order.setDiscountAmount(discount);
        order.setDiscountReason(
                request.reason() == null || request.reason().isBlank()
                        ? null
                        : request.reason().trim()
        );
        recalculateTotal(order);
        CafeOrder saved = orderRepository.save(order);
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            AuditLog.record("DISCOUNT_APPLY", actor, "order:" + orderId, "SUCCESS: " + discount);
        }
        return saved;
    }

    @Transactional
    public CafeOrder clearDiscount(Long orderId) {
        CafeOrder order = getOpenOrder(orderId);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setDiscountReason(null);
        recalculateTotal(order);
        return orderRepository.save(order);
    }

    @Transactional
    public CafeOrder holdOrder(Long orderId) {
        CafeOrder order = getOpenOrder(orderId);
        if (order.getGameSession() != null) {
            throw new IllegalStateException(
                    "Session-attached orders remain open until session checkout"
            );
        }
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Empty orders cannot be held");
        }
        order.setStatus(OrderStatus.HELD);
        return orderRepository.save(order);
    }

    @Transactional
    public CafeOrder resumeOrder(Long orderId) {
        CafeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != OrderStatus.HELD) {
            throw new IllegalStateException("Only held orders can be resumed");
        }
        if (order.getGameSession() != null) {
            throw new IllegalStateException("Only standalone orders can be resumed");
        }
        order.setStatus(OrderStatus.OPEN);
        return orderRepository.save(order);
    }

    @Transactional
    public CafeOrder cancelOrder(Long orderId) {
        CafeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != OrderStatus.OPEN
                && order.getStatus() != OrderStatus.HELD) {
            throw new IllegalStateException("Only open or held orders can be cancelled");
        }
        if (order.getGameSession() != null) {
            throw new IllegalStateException(
                    "Session-attached orders cannot be cancelled from POS"
            );
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<CafeOrder> getHeldOrders() {
        return orderRepository.findByGameSessionIsNullAndStatusOrderByCreatedAtDesc(
                OrderStatus.HELD
        );
    }

    @Transactional(readOnly = true)
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
        BigDecimal subtotal = order.getItems()
                .stream()
                .map(item -> {
                    BigDecimal line = money(item.getUnitPriceSnapshot()
                            .multiply(BigDecimal.valueOf(item.getQuantity())));
                    item.setLineTotal(line);
                    return line;
                })
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        BigDecimal discount = money(order.getDiscountAmount());
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        order.setDiscountAmount(discount);
        order.setTotalAmount(money(subtotal.subtract(discount)));
    }

    private BigDecimal calculateSubtotal(CafeOrder order) {
        return order.getItems().stream()
                .map(item -> money(item.getUnitPriceSnapshot()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean hasDiscountPermission(String userRole) {
        if (userRole == null || userRole.isBlank()) return false;
        try {
            Role role = Role.valueOf(userRole.trim().toUpperCase());
            return settingsService.get().getDiscountAllowedRoles().contains(role);
        } catch (IllegalArgumentException notARole) {
            return false;
        }
    }

    private void cancelEmptyStandaloneOrder(CafeOrder order) {
        if (order.getItems().isEmpty()
                && order.getGameSession() == null) {
            order.setStatus(OrderStatus.CANCELLED);
        }
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
