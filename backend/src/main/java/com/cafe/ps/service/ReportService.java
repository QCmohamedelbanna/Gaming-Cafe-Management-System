package com.cafe.ps.service;

import com.cafe.ps.dto.CashierShiftSummary;
import com.cafe.ps.dto.DashboardSummary;
import com.cafe.ps.dto.PaymentMethodTotal;
import com.cafe.ps.dto.ProductSalesReport;
import com.cafe.ps.dto.ReportSummary;
import com.cafe.ps.dto.RevenueBreakdown;
import com.cafe.ps.dto.SessionEndingSoon;
import com.cafe.ps.entity.Bill;
import com.cafe.ps.entity.BillStatus;
import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.OrderItem;
import com.cafe.ps.entity.Payment;
import com.cafe.ps.entity.PaymentMethod;
import com.cafe.ps.entity.PaymentStatus;
import com.cafe.ps.entity.Product;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.StockMovement;
import com.cafe.ps.entity.StockMovementType;
import com.cafe.ps.repository.BillRepository;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import com.cafe.ps.repository.ProductRepository;
import com.cafe.ps.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final BillRepository billRepository;
    private final DeviceRepository deviceRepository;
    private final GameSessionRepository sessionRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    @Value("${dashboard.ending-soon-minutes:30}")
    private int endingSoonMinutes;

    @Transactional(readOnly = true)
    public DashboardSummary dashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        List<Bill> paidBills = paidBills(from, to);
        RevenueTotals revenue = revenueTotals(paidBills);

        List<GameSession> todaySessions = sessionRepository.findByStartTimeBetween(from, to);
        List<GameSession> activeSessions = sessionRepository.findByStatus(SessionStatus.ACTIVE);
        List<Device> devices = currentDevices();

        long availableDevices = devices.stream()
                .filter(device -> device.getStatus() == DeviceStatus.AVAILABLE)
                .filter(device -> !Boolean.FALSE.equals(device.getActive()))
                .count();
        long offlineDevices = devices.stream()
                .filter(device -> device.getStatus() == DeviceStatus.OFFLINE)
                .count();
        long lowStockProducts = productRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .filter(Product::isLowStock)
                .count();
        long completedSessionsToday = todaySessions.stream()
                .filter(session -> session.getStatus() == SessionStatus.COMPLETED)
                .count();

        BigDecimal average = average(revenue.totalRevenue, paidBills.size());
        return new DashboardSummary(
                devices.size(),
                activeSessions.size(),
                completedSessionsToday,
                revenue.totalRevenue,
                revenue.gamingRevenue,
                revenue.cafeRevenue,
                paidBills.size(),
                availableDevices,
                offlineDevices,
                revenue.cafeRevenue,
                revenue.totalRevenue,
                paidBills.size(),
                average,
                lowStockProducts,
                endingSoon(activeSessions),
                paymentTotals(paidBills)
        );
    }

    @Transactional(readOnly = true)
    public ReportSummary report(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate to = requestedTo == null ? LocalDate.now() : requestedTo;
        LocalDate from = requestedFrom == null ? to : requestedFrom;
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Report end date cannot be before the start date");
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<Bill> paidBills = paidBills(start, end);
        List<Bill> cancelledBills = distinctBills(
                billRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                        BillStatus.CANCELLED,
                        start,
                        end
                )
        );
        List<Bill> refundedBills = distinctBills(
                billRepository.findByStatusAndRefundedAtBetweenOrderByRefundedAtAsc(
                        BillStatus.REFUNDED,
                        start,
                        end
                )
        );

        RevenueTotals revenue = revenueTotals(paidBills);
        BigDecimal discounts = paidBills.stream()
                .map(this::discountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportSummary(
                from,
                to,
                revenue.gamingRevenue,
                revenue.cafeRevenue,
                revenue.totalRevenue,
                money(discounts),
                paidBills.size(),
                average(revenue.totalRevenue, paidBills.size()),
                cancelledBills.size(),
                sumBillAmounts(cancelledBills),
                refundedBills.size(),
                sumBillAmounts(refundedBills),
                paymentTotals(paidBills),
                revenueByDevice(paidBills),
                revenueBySessionType(paidBills),
                productSales(paidBills),
                cashierShifts(paidBills, refundedBills)
        );
    }

    private List<Bill> paidBills(LocalDateTime from, LocalDateTime to) {
        return distinctBills(billRepository.findByStatusAndPaidAtBetweenOrderByPaidAtAsc(
                BillStatus.PAID,
                from,
                to
        ));
    }

    private List<Bill> distinctBills(List<Bill> bills) {
        return bills.stream().distinct().toList();
    }

    private List<Device> currentDevices() {
        return deviceRepository.findAllByDeletedFalseOrDeletedIsNull();
    }

    private List<SessionEndingSoon> endingSoon(List<GameSession> sessions) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusMinutes(Math.max(1, endingSoonMinutes));

        return sessions.stream()
                .map(session -> {
                    LocalDateTime endsAt = sessionEndTime(session);
                    if (endsAt == null || endsAt.isBefore(now) || endsAt.isAfter(horizon)) {
                        return null;
                    }
                    long seconds = Math.max(0, Duration.between(now, endsAt).getSeconds());
                    return new SessionEndingSoon(
                            session.getId(),
                            session.getDevice() == null ? "Unknown device" : session.getDevice().getName(),
                            session.getSessionType(),
                            endsAt,
                            seconds
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SessionEndingSoon::endsAt))
                .toList();
    }

    private LocalDateTime sessionEndTime(GameSession session) {
        if (session.getSessionType() == com.cafe.ps.entity.SessionType.MATCH
                || session.getBillingUnit() == com.cafe.ps.entity.BillingUnit.MATCH) {
            return session.getCurrentMatchExpiresAt();
        }
        if (session.getPlannedMinutes() == null || session.getStartTime() == null) return null;
        return session.getStartTime().plusMinutes(session.getPlannedMinutes());
    }

    private List<PaymentMethodTotal> paymentTotals(List<Bill> bills) {
        Map<PaymentMethod, MoneyCount> totals = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            totals.put(method, new MoneyCount());
        }

        for (Bill bill : bills) {
            for (Payment payment : bill.getPayments()) {
                if (payment.getStatus() != PaymentStatus.COMPLETED || payment.getMethod() == null) continue;
                MoneyCount total = totals.get(payment.getMethod());
                total.amount = total.amount.add(nz(payment.getAmount()));
                total.count++;
            }
        }

        return List.of(
                new PaymentMethodTotal(PaymentMethod.CASH, money(totals.get(PaymentMethod.CASH).amount), totals.get(PaymentMethod.CASH).count),
                new PaymentMethodTotal(PaymentMethod.CARD, money(totals.get(PaymentMethod.CARD).amount), totals.get(PaymentMethod.CARD).count),
                new PaymentMethodTotal(PaymentMethod.MOBILE_WALLET, money(totals.get(PaymentMethod.MOBILE_WALLET).amount), totals.get(PaymentMethod.MOBILE_WALLET).count)
        );
    }

    private List<RevenueBreakdown> revenueByDevice(List<Bill> bills) {
        Map<String, RevenueAccumulator> grouped = new LinkedHashMap<>();
        for (Bill bill : bills) {
            String label = bill.getSession() == null || bill.getSession().getDevice() == null
                    ? "Standalone café"
                    : bill.getSession().getDevice().getName();
            addRevenue(grouped, label, bill);
        }
        return toRevenueBreakdown(grouped);
    }

    private List<RevenueBreakdown> revenueBySessionType(List<Bill> bills) {
        Map<String, RevenueAccumulator> grouped = new LinkedHashMap<>();
        for (Bill bill : bills) {
            String label = bill.getSession() == null || bill.getSession().getSessionType() == null
                    ? "Standalone café"
                    : bill.getSession().getSessionType().name();
            addRevenue(grouped, label, bill);
        }
        return toRevenueBreakdown(grouped);
    }

    private void addRevenue(Map<String, RevenueAccumulator> grouped, String label, Bill bill) {
        RevenueAccumulator accumulator = grouped.computeIfAbsent(label, ignored -> new RevenueAccumulator());
        accumulator.gamingRevenue = accumulator.gamingRevenue.add(nz(bill.getGamingAmount()));
        accumulator.cafeRevenue = accumulator.cafeRevenue.add(nz(bill.getOrderAmount()));
        accumulator.totalRevenue = accumulator.totalRevenue.add(nz(bill.getTotalAmount()));
        accumulator.billCount++;
    }

    private List<RevenueBreakdown> toRevenueBreakdown(Map<String, RevenueAccumulator> grouped) {
        return grouped.entrySet().stream()
                .map(entry -> new RevenueBreakdown(
                        entry.getKey(),
                        money(entry.getValue().gamingRevenue),
                        money(entry.getValue().cafeRevenue),
                        money(entry.getValue().totalRevenue),
                        entry.getValue().billCount
                ))
                .sorted(Comparator.comparing(RevenueBreakdown::totalRevenue).reversed())
                .toList();
    }

    private List<ProductSalesReport> productSales(List<Bill> bills) {
        if (bills.isEmpty()) return List.of();

        Collection<String> references = bills.stream()
                .map(Bill::getBillNumber)
                .filter(Objects::nonNull)
                .toList();
        Map<String, BigDecimal> saleCosts = new HashMap<>();
        if (!references.isEmpty()) {
            for (StockMovement movement : movementRepository.findByTypeAndReferenceIn(
                    StockMovementType.SALE,
                    references
            )) {
                if (movement.getProduct() == null || movement.getReference() == null) continue;
                BigDecimal cost = nz(movement.getUnitCost())
                        .multiply(nz(movement.getQuantity()).abs());
                saleCosts.put(costKey(movement.getReference(), movement.getProduct().getId()), cost);
            }
        }

        Map<Long, ProductAccumulator> grouped = new LinkedHashMap<>();
        for (Bill bill : bills) {
            CafeOrder order = bill.getOrder();
            if (order == null || order.getItems() == null || order.getItems().isEmpty()) continue;

            BigDecimal subtotal = order.getItems().stream()
                    .map(this::lineGross)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal discount = discountAmount(bill);

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product == null || product.getId() == null) continue;

                BigDecimal gross = lineGross(item);
                BigDecimal allocatedDiscount = subtotal.signum() == 0
                        ? BigDecimal.ZERO
                        : money(discount.multiply(gross).divide(subtotal, 8, MONEY_ROUNDING));
                BigDecimal cost = saleCosts.get(costKey(bill.getBillNumber(), product.getId()));
                if (cost == null) {
                    cost = nz(product.getCostPrice()).multiply(BigDecimal.valueOf(valueOrZero(item.getQuantity())));
                }

                ProductAccumulator accumulator = grouped.computeIfAbsent(
                        product.getId(),
                        ignored -> new ProductAccumulator(product)
                );
                accumulator.quantity = accumulator.quantity.add(BigDecimal.valueOf(valueOrZero(item.getQuantity())));
                accumulator.grossSales = accumulator.grossSales.add(gross);
                accumulator.discountAmount = accumulator.discountAmount.add(allocatedDiscount);
                accumulator.costAmount = accumulator.costAmount.add(cost);
            }
        }

        return grouped.values().stream()
                .map(ProductAccumulator::toReport)
                .sorted(Comparator.comparing(ProductSalesReport::netSales).reversed())
                .toList();
    }

    private List<CashierShiftSummary> cashierShifts(List<Bill> paidBills, List<Bill> refundedBills) {
        Map<String, CashierAccumulator> grouped = new LinkedHashMap<>();
        for (Bill bill : paidBills) {
            for (Payment payment : bill.getPayments()) {
                if (payment.getStatus() != PaymentStatus.COMPLETED) continue;
                CashierAccumulator accumulator = grouped.computeIfAbsent(cashier(payment), ignored -> new CashierAccumulator());
                BigDecimal amount = nz(payment.getAmount());
                accumulator.totalCollected = accumulator.totalCollected.add(amount);
                accumulator.paymentCount++;
                if (payment.getMethod() == PaymentMethod.CASH) accumulator.cashCollected = accumulator.cashCollected.add(amount);
                if (payment.getMethod() == PaymentMethod.CARD) accumulator.cardCollected = accumulator.cardCollected.add(amount);
                if (payment.getMethod() == PaymentMethod.MOBILE_WALLET) accumulator.mobileWalletCollected = accumulator.mobileWalletCollected.add(amount);
            }
        }
        for (Bill bill : refundedBills) {
            for (Payment payment : bill.getPayments()) {
                CashierAccumulator accumulator = grouped.computeIfAbsent(cashier(payment), ignored -> new CashierAccumulator());
                accumulator.refunds = accumulator.refunds.add(nz(payment.getAmount()));
            }
        }

        return grouped.entrySet().stream()
                .map(entry -> entry.getValue().toSummary(entry.getKey()))
                .sorted(Comparator.comparing(CashierShiftSummary::netCollected).reversed())
                .toList();
    }

    private RevenueTotals revenueTotals(List<Bill> bills) {
        BigDecimal gaming = bills.stream().map(bill -> nz(bill.getGamingAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cafe = bills.stream().map(bill -> nz(bill.getOrderAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RevenueTotals(money(gaming), money(cafe), money(gaming.add(cafe)));
    }

    private BigDecimal sumBillAmounts(List<Bill> bills) {
        return money(bills.stream().map(bill -> nz(bill.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal discountAmount(Bill bill) {
        return bill.getOrder() == null ? BigDecimal.ZERO : money(bill.getOrder().getDiscountAmount());
    }

    private BigDecimal lineGross(OrderItem item) {
        return money(nz(item.getUnitPriceSnapshot()).multiply(BigDecimal.valueOf(valueOrZero(item.getQuantity()))));
    }

    private static String costKey(String reference, Long productId) {
        return reference + "|" + productId;
    }

    private static String cashier(Payment payment) {
        return payment.getCashier() == null || payment.getCashier().isBlank()
                ? "Admin"
                : payment.getCashier();
    }

    private static BigDecimal average(BigDecimal amount, long count) {
        return count == 0
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING)
                : money(amount.divide(BigDecimal.valueOf(count), 8, MONEY_ROUNDING));
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return nz(value).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private static final class RevenueTotals {
        private final BigDecimal gamingRevenue;
        private final BigDecimal cafeRevenue;
        private final BigDecimal totalRevenue;

        private RevenueTotals(BigDecimal gamingRevenue, BigDecimal cafeRevenue, BigDecimal totalRevenue) {
            this.gamingRevenue = gamingRevenue;
            this.cafeRevenue = cafeRevenue;
            this.totalRevenue = totalRevenue;
        }
    }

    private static final class MoneyCount {
        private BigDecimal amount = BigDecimal.ZERO;
        private long count;
    }

    private static final class RevenueAccumulator {
        private BigDecimal gamingRevenue = BigDecimal.ZERO;
        private BigDecimal cafeRevenue = BigDecimal.ZERO;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private long billCount;
    }

    private static final class ProductAccumulator {
        private final Product product;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal grossSales = BigDecimal.ZERO;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private BigDecimal costAmount = BigDecimal.ZERO;

        private ProductAccumulator(Product product) {
            this.product = product;
        }

        private ProductSalesReport toReport() {
            BigDecimal gross = money(grossSales);
            BigDecimal discount = money(discountAmount);
            BigDecimal net = money(gross.subtract(discount));
            BigDecimal cost = money(costAmount);
            return new ProductSalesReport(
                    product.getId(),
                    product.getName(),
                    product.getSku(),
                    product.getUnit(),
                    quantity.setScale(3, MONEY_ROUNDING),
                    gross,
                    discount,
                    net,
                    cost,
                    money(net.subtract(cost))
            );
        }
    }

    private static final class CashierAccumulator {
        private BigDecimal totalCollected = BigDecimal.ZERO;
        private BigDecimal cashCollected = BigDecimal.ZERO;
        private BigDecimal cardCollected = BigDecimal.ZERO;
        private BigDecimal mobileWalletCollected = BigDecimal.ZERO;
        private BigDecimal refunds = BigDecimal.ZERO;
        private long paymentCount;

        private CashierShiftSummary toSummary(String cashier) {
            BigDecimal net = totalCollected.subtract(refunds);
            return new CashierShiftSummary(
                    cashier,
                    money(totalCollected),
                    money(cashCollected),
                    money(cardCollected),
                    money(mobileWalletCollected),
                    money(refunds),
                    money(net),
                    paymentCount
            );
        }
    }
}
