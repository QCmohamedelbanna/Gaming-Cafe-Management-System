package com.cafe.ps.service;

import com.cafe.ps.dto.CheckoutLine;
import com.cafe.ps.dto.CheckoutResult;
import com.cafe.ps.entity.*;
import com.cafe.ps.repository.BillRepository;
import com.cafe.ps.repository.CafeOrderRepository;
import com.cafe.ps.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final BillRepository billRepository;
    private final CafeOrderRepository orderRepository;
    private final GameSessionRepository sessionRepository;
    private final InventoryService inventoryService;

    @Transactional
    public CheckoutResult checkoutSession(
            Long sessionId,
            PaymentMethod method,
            BigDecimal amountTendered
    ) {
        return checkoutSession(sessionId, method, amountTendered, "Admin");
    }

    @Transactional
    public CheckoutResult checkoutSession(
            Long sessionId,
            PaymentMethod method,
            BigDecimal amountTendered,
            String cashier
    ) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (session.getStatus() == SessionStatus.ACTIVE) {
            /*
             * Validate before changing the session or its order. This keeps
             * an invalid payment from leaving a partially completed checkout.
             */
            LocalDateTime endTime = LocalDateTime.now();
            CafeOrder order = findOpenOrder(sessionId);
            validatePayment(
                    method,
                    amountTendered,
                    calculateSessionAmount(session, endTime)
                            .add(calculateOrderAmount(order))
            );

            Bill bill = finalizeSession(sessionId, endTime, false);
            return settleBill(bill, method, amountTendered, cashier);
        }
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new IllegalStateException("Session cannot be billed");
        }
        if (session.getFinalAmount() == null) {
            throw new IllegalStateException(
                    "Completed session has no final calculation"
            );
        }

        Bill bill = billRepository.findBySessionId(sessionId).orElse(null);
        if (bill == null) {
            CafeOrder order = orderRepository
                    .findFirstByGameSessionIdAndStatusOrderByCreatedAtDesc(
                            sessionId,
                            OrderStatus.COMPLETED
                    )
                    .orElse(null);
            bill = ensureBill(
                    session,
                    order,
                    null,
                    false,
                    session.getEndTime() == null
                            ? LocalDateTime.now()
                            : session.getEndTime()
            );
        }

        return settleBill(bill, method, amountTendered, cashier);
    }

    @Transactional
    public CheckoutResult checkoutOrder(
            Long orderId,
            PaymentMethod method,
            BigDecimal amountTendered
    ) {
        return checkoutOrder(orderId, method, amountTendered, "Admin");
    }

    @Transactional
    public CheckoutResult checkoutOrder(
            Long orderId,
            PaymentMethod method,
            BigDecimal amountTendered,
            String cashier
    ) {
        CafeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getGameSession() != null) {
            throw new IllegalStateException(
                    "Session-attached orders are paid during session checkout"
            );
        }

        if (order.getStatus() != OrderStatus.OPEN) {
            Bill existing = billRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalStateException("Order is not open"));
            return settleBill(existing, method, amountTendered, cashier);
        }

        completeOrder(order);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Empty orders cannot be checked out");
        }

        Bill bill = ensureBill(
                order.getGameSession(),
                order,
                null,
                false,
                LocalDateTime.now()
        );
        return settleBill(bill, method, amountTendered, cashier);
    }

    /**
     * Used by planned-session expiry and final-match completion. The session
     * and any non-empty attached order are finalized atomically, while payment
     * remains pending until a cashier settles the generated bill.
     */
    @Transactional
    public Bill finalizeSession(Long sessionId, LocalDateTime endTime) {
        return finalizeSession(sessionId, endTime, false);
    }

    @Transactional
    public Bill finalizeSession(
            Long sessionId,
            LocalDateTime endTime,
            boolean automaticExpiry
    ) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        Bill existing = billRepository.findBySessionId(sessionId).orElse(null);
        if (session.getStatus() == SessionStatus.ACTIVE) {
            completeSession(session, endTime);
            CafeOrder order = findOpenOrder(sessionId);
            completeOrder(order);
            if (order == null && existing != null) {
                order = existing.getOrder();
            }
            return ensureBill(session, order, existing, automaticExpiry, endTime);
        }

        if (existing != null) return existing;

        CafeOrder order = orderRepository
                .findFirstByGameSessionIdAndStatusOrderByCreatedAtDesc(
                        sessionId,
                        OrderStatus.COMPLETED
                )
                .orElse(null);
        return ensureBill(session, order, null, automaticExpiry, endTime);
    }

    @Transactional(readOnly = true)
    public CheckoutResult getBill(Long billId) {
        Bill bill = billRepository.findDetailedById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));
        return toResult(bill);
    }

    @Transactional(readOnly = true)
    public List<CheckoutResult> getPendingBills() {
        return billRepository
                .findByStatusOrderByCreatedAtDesc(BillStatus.PENDING_PAYMENT)
                .stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CheckoutResult> getActiveExpiryAlerts() {
        return billRepository
                .findByStatusAndAutomaticExpiryTrueAndNotificationExpiresAtAfterOrderByCreatedAtDesc(
                        BillStatus.PENDING_PAYMENT,
                        LocalDateTime.now()
                )
                .stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional
    public CheckoutResult payBill(
            Long billId,
            PaymentMethod method,
            BigDecimal amountTendered
    ) {
        return payBill(billId, method, amountTendered, "Admin");
    }

    @Transactional
    public CheckoutResult payBill(
            Long billId,
            PaymentMethod method,
            BigDecimal amountTendered,
            String cashier
    ) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));
        return settleBill(bill, method, amountTendered, cashier);
    }

    @Transactional
    public CheckoutResult cancelBill(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        if (bill.getStatus() != BillStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only pending bills can be cancelled");
        }

        bill.setStatus(BillStatus.CANCELLED);
        if (bill.getOrder() != null
                && bill.getOrder().getStatus() == OrderStatus.COMPLETED) {
            bill.getOrder().setStatus(OrderStatus.CANCELLED);
        }

        return toResult(billRepository.save(bill));
    }

    @Transactional
    public CheckoutResult refund(Long billId, String reason) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        if (bill.getStatus() != BillStatus.PAID) {
            throw new IllegalStateException("Only paid bills can be refunded");
        }

        bill.setStatus(BillStatus.REFUNDED);
        bill.setRefundedAt(LocalDateTime.now());
        bill.setRefundReason(reason == null ? "Refunded by administrator" : reason.trim());
        inventoryService.recordRefund(
                bill.getBillNumber(),
                "REFUND-" + bill.getBillNumber()
        );
        bill.getPayments().forEach(payment ->
                payment.setStatus(PaymentStatus.REFUNDED)
        );

        return toResult(billRepository.save(bill));
    }

    private CafeOrder findOpenOrder(Long sessionId) {
        return orderRepository
                .findFirstByGameSessionIdAndStatusOrderByCreatedAtDesc(
                        sessionId,
                        OrderStatus.OPEN
                )
                .orElse(null);
    }

    private void completeOrder(CafeOrder order) {
        if (order == null) return;

        if (order.getItems().isEmpty()) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            return;
        }

        BigDecimal total = calculateOrderAmount(order);
        order.getItems().forEach(item -> item.setLineTotal(
                money(item.getUnitPriceSnapshot()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
        ));

        order.setTotalAmount(money(total));
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private void completeSession(GameSession session, LocalDateTime endTime) {
        if (session.getStatus() != SessionStatus.ACTIVE) return;

        BigDecimal amount = calculateSessionAmount(session, endTime);
        session.setEndTime(endTime);
        session.setFinalAmount(amount);
        session.setStatus(SessionStatus.COMPLETED);
        if (session.getDevice() != null) {
            session.getDevice().setStatus(DeviceStatus.AVAILABLE);
        }
        sessionRepository.save(session);
    }

    private BigDecimal calculateSessionAmount(
            GameSession session,
            LocalDateTime endTime
    ) {
        BigDecimal amount;
        if (session.getSessionType() == SessionType.MATCH
                || session.getBillingUnit() == BillingUnit.MATCH) {
            amount = money(nz(session.getUnitPriceSnapshot())
                    .multiply(BigDecimal.valueOf(valueOrOne(session.getPurchasedMatches()))));
        } else {
            long seconds = Math.max(
                    1,
                    java.time.Duration.between(session.getStartTime(), endTime).getSeconds()
            );
            if (session.getPlannedMinutes() != null) {
                seconds = Math.min(seconds, session.getPlannedMinutes() * 60L);
            }

            BigDecimal hours = BigDecimal.valueOf(seconds)
                    .divide(BigDecimal.valueOf(3600), 8, MONEY_ROUNDING);
            BigDecimal rate = session.getUnitPriceSnapshot() != null
                    ? session.getUnitPriceSnapshot()
                    : session.getHourlyRateSnapshot();
            amount = money(nz(rate).multiply(hours));
        }

        return amount;
    }

    private BigDecimal calculateOrderAmount(CafeOrder order) {
        if (order == null || order.getItems().isEmpty()) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        }

        BigDecimal subtotal = order.getItems().stream()
                .map(item -> money(
                        item.getUnitPriceSnapshot()
                                .multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = money(order.getDiscountAmount());
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        return money(subtotal.subtract(discount));
    }

    private Bill ensureBill(
            GameSession session,
            CafeOrder order,
            Bill existing,
            boolean automaticExpiry,
            LocalDateTime endTime
    ) {
        Bill bill = existing;
        if (bill == null && order != null) {
            bill = billRepository.findByOrderId(order.getId()).orElse(null);
        }

        BigDecimal gamingAmount = session == null
                ? BigDecimal.ZERO
                : money(session.getFinalAmount());
        BigDecimal orderAmount = order == null
                ? BigDecimal.ZERO
                : money(order.getTotalAmount());

        if (bill == null) {
            bill = Bill.builder()
                    .billNumber("TEMP-" + UUID.randomUUID())
                    .session(session)
                    .order(order)
                    .gamingAmount(gamingAmount)
                    .orderAmount(orderAmount)
                    .totalAmount(money(gamingAmount.add(orderAmount)))
                    .status(BillStatus.PENDING_PAYMENT)
                    .createdAt(LocalDateTime.now())
                    .build();
            bill = billRepository.save(bill);
            bill.setBillNumber(String.format("BILL-%08d", bill.getId()));
        } else {
            bill.setSession(session);
            bill.setOrder(order);
            bill.setGamingAmount(gamingAmount);
            bill.setOrderAmount(orderAmount);
            bill.setTotalAmount(money(gamingAmount.add(orderAmount)));
        }

        if (automaticExpiry) {
            bill.setAutomaticExpiry(true);
            if (bill.getNotificationExpiresAt() == null) {
                bill.setNotificationExpiresAt(endTime.plusMinutes(2));
            }
        }

        return billRepository.save(bill);
    }

    private CheckoutResult settleBill(
            Bill bill,
            PaymentMethod method,
            BigDecimal amountTendered,
            String cashier
    ) {
        if (bill.getStatus() == BillStatus.REFUNDED
                || bill.getStatus() == BillStatus.CANCELLED) {
            throw new IllegalStateException("Bill is not payable");
        }

        if (bill.getStatus() == BillStatus.PAID) {
            return toResult(bill);
        }

        BigDecimal total = money(bill.getTotalAmount());
        validatePayment(method, amountTendered, total);

        // A missing amountTendered intentionally means exact payment.
        BigDecimal tendered = amountTendered == null ? total : money(amountTendered);

        inventoryService.recordSale(
                bill.getOrder(),
                bill.getBillNumber()
        );

        BigDecimal change = method == PaymentMethod.CASH
                ? money(tendered.subtract(total))
                : BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);

        Payment payment = Payment.builder()
                .method(method)
                .amount(total)
                .amountTendered(tendered)
                .changeAmount(change)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .cashier(normalizeCashier(cashier))
                .build();
        bill.addPayment(payment);
        bill.setStatus(BillStatus.PAID);
        bill.setPaidAt(payment.getPaidAt());

        return toResult(billRepository.save(bill));
    }

    private String normalizeCashier(String cashier) {
        return cashier == null || cashier.isBlank() ? "Admin" : cashier.trim();
    }

    private void validatePayment(
            PaymentMethod method,
            BigDecimal amountTendered,
            BigDecimal total
    ) {
        if (method == null) {
            throw new IllegalArgumentException("Payment method is required");
        }

        BigDecimal tendered = amountTendered == null
                ? total
                : money(amountTendered);

        if (tendered.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount tendered cannot be negative");
        }
        if (method != PaymentMethod.CASH
                && tendered.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "Card and mobile-wallet payments must equal the bill total"
            );
        }
        if (tendered.compareTo(total) < 0) {
            throw new IllegalArgumentException("Amount tendered is less than the bill total");
        }
    }

    private CheckoutResult toResult(Bill bill) {
        Payment payment = bill.getPayments().stream().findFirst().orElse(null);
        List<CheckoutLine> lines = bill.getOrder() == null
                ? List.of()
                : bill.getOrder().getItems().stream()
                .map(item -> new CheckoutLine(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        money(item.getUnitPriceSnapshot()),
                        money(item.getLineTotal())
                ))
                .toList();

        BigDecimal orderSubtotal = bill.getOrder() == null
                ? BigDecimal.ZERO
                : bill.getOrder().getItems().stream()
                .map(item -> money(item.getUnitPriceSnapshot()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = bill.getOrder() == null
                ? BigDecimal.ZERO
                : money(bill.getOrder().getDiscountAmount());

        return new CheckoutResult(
                bill.getSession() == null ? null : bill.getSession().getId(),
                bill.getOrder() == null ? null : bill.getOrder().getId(),
                bill.getId(),
                bill.getBillNumber(),
                money(bill.getGamingAmount()),
                money(orderSubtotal),
                discountAmount,
                money(bill.getOrderAmount()),
                money(bill.getTotalAmount()),
                payment == null ? null : payment.getMethod(),
                payment == null ? null : money(payment.getAmountTendered()),
                payment == null ? null : money(payment.getChangeAmount()),
                bill.getPaidAt() == null ? bill.getCreatedAt() : bill.getPaidAt(),
                bill.getStatus(),
                lines,
                bill.getSession() == null || bill.getSession().getDevice() == null
                        ? null
                        : bill.getSession().getDevice().getName(),
                Boolean.TRUE.equals(bill.getAutomaticExpiry()),
                bill.getNotificationExpiresAt()
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return nz(value).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static int valueOrOne(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }
}
