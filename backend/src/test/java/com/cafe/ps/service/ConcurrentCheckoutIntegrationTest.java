package com.cafe.ps.service;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import com.cafe.ps.dto.CheckoutResult;
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
import com.cafe.ps.entity.StockMovement;
import com.cafe.ps.entity.StockMovementType;
import com.cafe.ps.repository.BillRepository;
import com.cafe.ps.repository.CafeOrderRepository;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import com.cafe.ps.repository.OrderItemRepository;
import com.cafe.ps.repository.PaymentRepository;
import com.cafe.ps.repository.ProductRepository;
import com.cafe.ps.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the session-finalization and payment race on real MySQL
 * connections. Each service call in a worker owns an independent Spring
 * transaction, so the assertions cover persisted state rather than only
 * returned Java objects.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false"
        }
)
class ConcurrentCheckoutIntegrationTest extends AbstractMySQLIntegrationTest {

    private static final BigDecimal GAMING_AMOUNT = new BigDecimal("40.00");
    private static final BigDecimal ORDER_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal BILL_TOTAL = new BigDecimal("50.00");
    private static final LocalDateTime FINALIZATION_TIME =
            LocalDateTime.of(2026, 1, 1, 13, 0);

    @Autowired
    private BillingService billingService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private CafeOrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @BeforeEach
    void cleanDatabase() {
        stockMovementRepository.deleteAll();
        paymentRepository.deleteAll();
        billRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        sessionRepository.deleteAll();
        productRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void concurrentAutomaticFinalizationAndManualCheckoutCreateOnePaidBill()
            throws Exception {
        SessionFixture fixture = saveSessionWithOrder();

        runConcurrently(
                () -> {
                    billingService.finalizeSession(
                            fixture.session().getId(),
                            FINALIZATION_TIME,
                            true
                    );
                    return null;
                },
                () -> {
                    billingService.checkoutSession(
                            fixture.session().getId(),
                            PaymentMethod.CASH,
                            BILL_TOTAL,
                            "Admin"
                    );
                    return null;
                }
        );

        assertPersistedPaidSession(fixture);
    }

    @Test
    void twoConcurrentFinalizationsCreateOnePendingBillAndCompleteOrderOnce()
            throws Exception {
        SessionFixture fixture = saveSessionWithOrder();

        List<Bill> returnedBills = runConcurrently(
                () -> billingService.finalizeSession(
                        fixture.session().getId(),
                        FINALIZATION_TIME,
                        true
                ),
                () -> billingService.finalizeSession(
                        fixture.session().getId(),
                        FINALIZATION_TIME,
                        true
                )
        );

        assertThat(returnedBills)
                .extracting(Bill::getId)
                .containsOnly(returnedBills.get(0).getId());
        assertThat(billRepository.count()).isEqualTo(1);
        Bill bill = billRepository.findBySessionId(fixture.session().getId())
                .orElseThrow();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PENDING_PAYMENT);
        assertThat(bill.getGamingAmount()).isEqualByComparingTo(GAMING_AMOUNT);
        assertThat(bill.getOrderAmount()).isEqualByComparingTo(ORDER_AMOUNT);
        assertThat(bill.getTotalAmount())
                .isEqualByComparingTo(bill.getGamingAmount().add(bill.getOrderAmount()));
        assertThat(paymentRepository.count()).isZero();
        assertThat(stockMovementsOfType(StockMovementType.SALE)).isEmpty();
        assertSessionAndOrderFinalState(fixture);
    }

    @RepeatedTest(3)
    void twoSimultaneousCheckoutsCreateOnePaymentAndOneInventorySale()
            throws Exception {
        SessionFixture fixture = saveSessionWithOrder();

        List<CheckoutResult> results = runConcurrently(
                () -> billingService.checkoutSession(
                        fixture.session().getId(),
                        PaymentMethod.CASH,
                        BILL_TOTAL,
                        "Admin"
                ),
                () -> billingService.checkoutSession(
                        fixture.session().getId(),
                        PaymentMethod.CASH,
                        BILL_TOTAL,
                        "Admin"
                )
        );

        assertThat(results)
                .extracting(CheckoutResult::status)
                .containsOnly(BillStatus.PAID);
        assertThat(results)
                .extracting(CheckoutResult::billId)
                .containsOnly(results.get(0).billId());
        assertPersistedPaidSession(fixture);
    }

    @Test
    void checkoutWinsThenFinalizationReturnsTheExistingPaidBillUnchanged()
            throws Exception {
        SessionFixture fixture = saveSessionWithOrder();

        CheckoutResult paid = billingService.checkoutSession(
                fixture.session().getId(),
                PaymentMethod.CASH,
                BILL_TOTAL,
                "Admin"
        );
        Bill returned = billingService.finalizeSession(
                fixture.session().getId(),
                FINALIZATION_TIME,
                true
        );

        assertThat(returned.getId()).isEqualTo(paid.billId());
        assertPersistedPaidSession(fixture);
        assertThat(billRepository.findById(paid.billId()).orElseThrow()
                .getAutomaticExpiry()).isFalse();
    }

    @Test
    void finalizationWinsThenCheckoutPaysTheExistingPendingBill()
            throws Exception {
        SessionFixture fixture = saveSessionWithOrder();

        Bill pending = billingService.finalizeSession(
                fixture.session().getId(),
                FINALIZATION_TIME,
                true
        );

        assertThat(pending.getStatus()).isEqualTo(BillStatus.PENDING_PAYMENT);
        assertThat(paymentRepository.count()).isZero();
        assertThat(stockMovementsOfType(StockMovementType.SALE)).isEmpty();

        CheckoutResult paid = billingService.checkoutSession(
                fixture.session().getId(),
                PaymentMethod.CASH,
                BILL_TOTAL,
                "Admin"
        );

        assertThat(paid.billId()).isEqualTo(pending.getId());
        assertPersistedPaidSession(fixture);
    }

    private void assertPersistedPaidSession(SessionFixture fixture) {
        assertThat(billRepository.count()).isEqualTo(1);
        Bill bill = billRepository.findBySessionId(fixture.session().getId())
                .orElseThrow();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(bill.getGamingAmount()).isEqualByComparingTo(GAMING_AMOUNT);
        assertThat(bill.getOrderAmount()).isEqualByComparingTo(ORDER_AMOUNT);
        assertThat(bill.getTotalAmount())
                .isEqualByComparingTo(bill.getGamingAmount().add(bill.getOrderAmount()));

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payments.get(0).getAmount()).isEqualByComparingTo(BILL_TOTAL);

        assertThat(stockMovementsOfType(StockMovementType.SALE)).hasSize(1);
        StockMovement sale = stockMovementsOfType(StockMovementType.SALE).get(0);
        assertThat(sale.getQuantity()).isEqualByComparingTo("-2.000");
        assertThat(sale.getReference()).isEqualTo(bill.getBillNumber());
        assertThat(productRepository.findById(fixture.product().getId()).orElseThrow()
                .getCurrentStock()).isEqualByComparingTo("8.000");

        assertSessionAndOrderFinalState(fixture);
    }

    private void assertSessionAndOrderFinalState(SessionFixture fixture) {
        assertThat(sessionRepository.count()).isEqualTo(1);
        GameSession session = sessionRepository.findById(fixture.session().getId())
                .orElseThrow();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getFinalAmount()).isEqualByComparingTo(GAMING_AMOUNT);
        assertThat(session.getEndTime()).isNotNull();
        assertThat(deviceRepository.findById(fixture.device().getId()).orElseThrow()
                .getStatus()).isEqualTo(DeviceStatus.AVAILABLE);

        assertThat(orderRepository.count()).isEqualTo(1);
        CafeOrder order = orderRepository.findById(fixture.order().getId())
                .orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(ORDER_AMOUNT);
        assertThat(order.getCompletedAt()).isNotNull();
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getLineTotal())
                .isEqualByComparingTo(ORDER_AMOUNT);
    }

    private List<StockMovement> stockMovementsOfType(StockMovementType type) {
        return stockMovementRepository.findAll().stream()
                .filter(movement -> movement.getType() == type)
                .toList();
    }

    private SessionFixture saveSessionWithOrder() {
        Device device = deviceRepository.save(Device.builder()
                .name("CONCURRENT-TEST-" + UUID.randomUUID())
                .type(DeviceType.PS4)
                .status(DeviceStatus.PLAYING)
                .active(true)
                .deleted(false)
                .build());

        GameSession session = sessionRepository.save(GameSession.builder()
                .device(device)
                .startTime(FINALIZATION_TIME.minusHours(1))
                .hourlyRateSnapshot(GAMING_AMOUNT)
                .unitPriceSnapshot(GAMING_AMOUNT)
                .sessionType(SessionType.MATCH)
                .billingUnit(BillingUnit.MATCH)
                .purchasedMatches(1)
                .completedMatches(0)
                .matchDurationMinutesSnapshot(15)
                .matchExpired(false)
                .status(SessionStatus.ACTIVE)
                .build());

        Product product = productRepository.save(Product.builder()
                .name("CONCURRENT-PRODUCT-" + UUID.randomUUID())
                .price(new BigDecimal("5.00"))
                .sellingPrice(new BigDecimal("5.00"))
                .costPrice(new BigDecimal("1.00"))
                .trackStock(true)
                .currentStock(new BigDecimal("10.000"))
                .active(true)
                .deleted(false)
                .build());

        CafeOrder order = CafeOrder.builder()
                .gameSession(session)
                .createdAt(FINALIZATION_TIME.minusMinutes(30))
                .status(OrderStatus.OPEN)
                .totalAmount(BigDecimal.ZERO)
                .build();
        order.addItem(OrderItem.builder()
                .product(product)
                .quantity(2)
                .unitPriceSnapshot(new BigDecimal("5.00"))
                .lineTotal(BigDecimal.ZERO)
                .build());
        order = orderRepository.save(order);

        return new SessionFixture(session, device, product, order);
    }

    private <T> List<T> runConcurrently(
            Callable<T> first,
            Callable<T> second
    ) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<T>> futures = List.of(
                    pool.submit(() -> awaitAndRun(ready, go, first)),
                    pool.submit(() -> awaitAndRun(ready, go, second))
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            return futures.stream()
                    .map(future -> get(future, 15, TimeUnit.SECONDS))
                    .toList();
        } finally {
            go.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private <T> T awaitAndRun(
            CountDownLatch ready,
            CountDownLatch go,
            Callable<T> action
    ) throws Exception {
        ready.countDown();
        if (!go.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent test workers did not start");
        }
        return action.call();
    }

    private <T> T get(Future<T> future, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (Exception exception) {
            throw new AssertionError("Concurrent service call failed", exception);
        }
    }

    private record SessionFixture(
            GameSession session,
            Device device,
            Product product,
            CafeOrder order
    ) {
    }
}
