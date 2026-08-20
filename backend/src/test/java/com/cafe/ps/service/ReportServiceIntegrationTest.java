package com.cafe.ps.service;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import com.cafe.ps.dto.DashboardSummary;
import com.cafe.ps.dto.PaymentMethodTotal;
import com.cafe.ps.dto.ProductSalesReport;
import com.cafe.ps.dto.ReportSummary;
import com.cafe.ps.entity.Bill;
import com.cafe.ps.entity.BillStatus;
import com.cafe.ps.entity.BillingUnit;
import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.OrderItem;
import com.cafe.ps.entity.OrderStatus;
import com.cafe.ps.entity.Payment;
import com.cafe.ps.entity.PaymentMethod;
import com.cafe.ps.entity.PaymentStatus;
import com.cafe.ps.entity.Product;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.repository.BillRepository;
import com.cafe.ps.repository.CafeOrderRepository;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import com.cafe.ps.repository.OrderItemRepository;
import com.cafe.ps.repository.PaymentRepository;
import com.cafe.ps.repository.ProductRepository;
import com.cafe.ps.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false"
        }
)
class ReportServiceIntegrationTest extends AbstractMySQLIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CafeOrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        billRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        movementRepository.deleteAll();
        sessionRepository.deleteAll();
        productRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void reportIncludesGamingCafeRevenuePaymentMethodsAndProfitability() {
        LocalDateTime now = LocalDateTime.now();
        Device device = saveDevice("REPORT-PS5", DeviceType.PS5, DeviceStatus.AVAILABLE);
        Product product = saveProduct("Report Cola", "10.00", "4.00", false, "Drinks");
        GameSession session = saveCompletedSession(device, now.minusHours(2), "50.00");
        CafeOrder attachedOrder = saveOrder(session, product, 1, "10.00", "0.00");

        saveBill(
                "50.00",
                "10.00",
                BillStatus.PAID,
                now.minusHours(1),
                now.minusHours(1),
                null,
                session,
                attachedOrder,
                PaymentMethod.CARD,
                "60.00",
                PaymentStatus.COMPLETED,
                "Cashier A"
        );
        saveBill(
                "0.00",
                "20.00",
                BillStatus.PAID,
                now.minusMinutes(30),
                now.minusMinutes(30),
                null,
                null,
                null,
                PaymentMethod.CASH,
                "20.00",
                PaymentStatus.COMPLETED,
                "Admin"
        );

        ReportSummary report = reportService.report(LocalDate.now(), LocalDate.now());

        assertThat(report.gamingRevenue()).isEqualByComparingTo("50.00");
        assertThat(report.cafeRevenue()).isEqualByComparingTo("30.00");
        assertThat(report.totalRevenue()).isEqualByComparingTo("80.00");
        assertThat(report.completedBills()).isEqualTo(2);
        assertThat(report.averageBillValue()).isEqualByComparingTo("40.00");

        PaymentMethodTotal cash = paymentTotal(report, PaymentMethod.CASH);
        PaymentMethodTotal card = paymentTotal(report, PaymentMethod.CARD);
        assertThat(cash.amount()).isEqualByComparingTo("20.00");
        assertThat(cash.transactionCount()).isEqualTo(1);
        assertThat(card.amount()).isEqualByComparingTo("60.00");
        assertThat(card.transactionCount()).isEqualTo(1);

        ProductSalesReport productReport = report.productSales().stream()
                .filter(item -> item.productId().equals(product.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(productReport.quantity()).isEqualByComparingTo("1.000");
        assertThat(productReport.netSales()).isEqualByComparingTo("10.00");
        assertThat(productReport.costAmount()).isEqualByComparingTo("4.00");
        assertThat(productReport.profit()).isEqualByComparingTo("6.00");

        assertThat(report.revenueByDevice())
                .anySatisfy(row -> {
                    assertThat(row.label()).isEqualTo("REPORT-PS5");
                    assertThat(row.totalRevenue()).isEqualByComparingTo("60.00");
                });
        assertThat(report.cashierShifts())
                .anySatisfy(row -> {
                    assertThat(row.cashier()).isEqualTo("Cashier A");
                    assertThat(row.netCollected()).isEqualByComparingTo("60.00");
                });
    }

    @Test
    void reportCountsRefundsCancellationsAndExcludesBillsOutsideRange() {
        LocalDateTime now = LocalDateTime.now();
        saveBill(
                "0.00",
                "15.00",
                BillStatus.REFUNDED,
                now.minusMinutes(30),
                now.minusMinutes(30),
                now.minusMinutes(10),
                null,
                null,
                PaymentMethod.CASH,
                "15.00",
                PaymentStatus.REFUNDED,
                "Admin"
        );
        saveBill(
                "0.00",
                "30.00",
                BillStatus.CANCELLED,
                now.minusMinutes(20),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        saveBill(
                "0.00",
                "99.00",
                BillStatus.PAID,
                now.minusDays(2),
                now.minusDays(2),
                null,
                null,
                null,
                PaymentMethod.CASH,
                "99.00",
                PaymentStatus.COMPLETED,
                "Admin"
        );

        ReportSummary report = reportService.report(LocalDate.now(), LocalDate.now());

        assertThat(report.completedBills()).isZero();
        assertThat(report.totalRevenue()).isEqualByComparingTo("0.00");
        assertThat(report.cancelledBills()).isEqualTo(1);
        assertThat(report.cancelledAmount()).isEqualByComparingTo("30.00");
        assertThat(report.refundedBills()).isEqualTo(1);
        assertThat(report.refundedAmount()).isEqualByComparingTo("15.00");
        assertThat(report.cashierShifts())
                .anySatisfy(row -> {
                    assertThat(row.cashier()).isEqualTo("Admin");
                    assertThat(row.refunds()).isEqualByComparingTo("15.00");
                    assertThat(row.netCollected()).isEqualByComparingTo("-15.00");
                });
    }

    @Test
    void dashboardExposesOperationalAndPersistedBillMetrics() {
        LocalDateTime now = LocalDateTime.now();
        saveDevice("AVAILABLE-REPORT", DeviceType.PS4, DeviceStatus.AVAILABLE);
        saveDevice("OFFLINE-REPORT", DeviceType.PS4, DeviceStatus.OFFLINE);
        Device playing = saveDevice("ENDING-REPORT", DeviceType.PS5, DeviceStatus.PLAYING);
        saveActiveSession(playing, now.minusMinutes(10), 30);
        saveProduct("Low Stock Report", "12.00", "5.00", true, "Snacks", "1.00", "3.00");
        saveBill(
                "0.00",
                "12.00",
                BillStatus.PAID,
                now.minusMinutes(5),
                now.minusMinutes(5),
                null,
                null,
                null,
                PaymentMethod.MOBILE_WALLET,
                "12.00",
                PaymentStatus.COMPLETED,
                "Admin"
        );

        DashboardSummary dashboard = reportService.dashboardSummary();

        assertThat(dashboard.activeSessions()).isEqualTo(1);
        assertThat(dashboard.availableDevices()).isEqualTo(1);
        assertThat(dashboard.offlineDevices()).isEqualTo(1);
        assertThat(dashboard.lowStockProducts()).isEqualTo(1);
        assertThat(dashboard.totalRevenueToday()).isEqualByComparingTo("12.00");
        assertThat(dashboard.completedBillsToday()).isEqualTo(1);
        assertThat(dashboard.sessionsEndingSoon())
                .anySatisfy(session -> assertThat(session.deviceName()).isEqualTo("ENDING-REPORT"));
        assertThat(paymentTotal(dashboard.salesByPaymentMethod(), PaymentMethod.MOBILE_WALLET).amount())
                .isEqualByComparingTo("12.00");
    }

    private PaymentMethodTotal paymentTotal(ReportSummary report, PaymentMethod method) {
        return paymentTotal(report.paymentMethods(), method);
    }

    private PaymentMethodTotal paymentTotal(java.util.List<PaymentMethodTotal> totals, PaymentMethod method) {
        return totals.stream()
                .filter(item -> item.method() == method)
                .findFirst()
                .orElseThrow();
    }

    private Device saveDevice(String name, DeviceType type, DeviceStatus status) {
        return deviceRepository.save(Device.builder()
                .name(name)
                .type(type)
                .status(status)
                .active(true)
                .deleted(false)
                .build());
    }

    private Product saveProduct(String name, String sellingPrice, String costPrice, boolean trackStock, String category) {
        return saveProduct(name, sellingPrice, costPrice, trackStock, category, "0.00", "0.00");
    }

    private Product saveProduct(
            String name,
            String sellingPrice,
            String costPrice,
            boolean trackStock,
            String category,
            String currentStock,
            String minimumStock
    ) {
        return productRepository.save(Product.builder()
                .name(name + "-" + UUID.randomUUID())
                .price(money(sellingPrice))
                .sellingPrice(money(sellingPrice))
                .costPrice(money(costPrice))
                .category(category)
                .trackStock(trackStock)
                .currentStock(money(currentStock))
                .minimumStock(money(minimumStock))
                .active(true)
                .deleted(false)
                .build());
    }

    private GameSession saveCompletedSession(Device device, LocalDateTime start, String finalAmount) {
        return sessionRepository.save(GameSession.builder()
                .device(device)
                .startTime(start)
                .endTime(start.plusHours(1))
                .plannedMinutes(60)
                .hourlyRateSnapshot(money(finalAmount))
                .unitPriceSnapshot(money(finalAmount))
                .sessionType(SessionType.SINGLE)
                .billingUnit(BillingUnit.HOUR)
                .finalAmount(money(finalAmount))
                .status(SessionStatus.COMPLETED)
                .build());
    }

    private GameSession saveActiveSession(Device device, LocalDateTime start, int plannedMinutes) {
        return sessionRepository.save(GameSession.builder()
                .device(device)
                .startTime(start)
                .plannedMinutes(plannedMinutes)
                .hourlyRateSnapshot(money("50.00"))
                .unitPriceSnapshot(money("50.00"))
                .sessionType(SessionType.SINGLE)
                .billingUnit(BillingUnit.HOUR)
                .status(SessionStatus.ACTIVE)
                .build());
    }

    private CafeOrder saveOrder(GameSession session, Product product, int quantity, String unitPrice, String discount) {
        CafeOrder order = CafeOrder.builder()
                .gameSession(session)
                .createdAt(LocalDateTime.now().minusHours(2))
                .completedAt(LocalDateTime.now().minusHours(1))
                .status(OrderStatus.COMPLETED)
                .discountAmount(money(discount))
                .totalAmount(money(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)).subtract(new BigDecimal(discount)).toString()))
                .build();
        order.addItem(OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPriceSnapshot(money(unitPrice))
                .lineTotal(money(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)).toString()))
                .build());
        return orderRepository.save(order);
    }

    private Bill saveBill(
            String gamingAmount,
            String orderAmount,
            BillStatus status,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            LocalDateTime refundedAt,
            GameSession session,
            CafeOrder order,
            PaymentMethod method,
            String paymentAmount,
            PaymentStatus paymentStatus,
            String cashier
    ) {
        Bill bill = Bill.builder()
                .billNumber("REPORT-" + UUID.randomUUID())
                .session(session)
                .order(order)
                .gamingAmount(money(gamingAmount))
                .orderAmount(money(orderAmount))
                .totalAmount(money(new BigDecimal(gamingAmount).add(new BigDecimal(orderAmount)).toString()))
                .status(status)
                .createdAt(createdAt)
                .paidAt(paidAt)
                .refundedAt(refundedAt)
                .build();

        if (method != null) {
            BigDecimal amount = money(paymentAmount);
            bill.addPayment(Payment.builder()
                    .method(method)
                    .amount(amount)
                    .amountTendered(amount)
                    .changeAmount(money("0.00"))
                    .status(paymentStatus)
                    .paidAt(paidAt == null ? createdAt : paidAt)
                    .cashier(cashier)
                    .build());
        }
        return billRepository.save(bill);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }
}
