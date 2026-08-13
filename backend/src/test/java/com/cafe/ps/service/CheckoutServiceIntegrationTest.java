package com.cafe.ps.service;

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
import com.cafe.ps.entity.PaymentMethod;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.entity.Product;
import com.cafe.ps.repository.BillRepository;
import com.cafe.ps.repository.CafeOrderRepository;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import com.cafe.ps.repository.OrderItemRepository;
import com.cafe.ps.repository.PaymentRepository;
import com.cafe.ps.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:checkout-tests?mode=memory&cache=shared",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false",
                "spring.datasource.hikari.maximum-pool-size=1"
        }
)
class CheckoutServiceIntegrationTest {

    private static final BigDecimal HOURLY_RATE = new BigDecimal("50.00");
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private CafeOrderRepository orderRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        billRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        sessionRepository.deleteAll();
        deviceRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void activeSessionWithoutOrderAndExactCashCompletesAndPays() {
        Device device = saveDevice("TEST-PS4-1", DeviceType.PS4);
        GameSession session = saveHourlySession(device, null, 60, START);

        CheckoutResult result = checkoutService.checkout(
                session.getId(),
                PaymentMethod.CASH,
                money("50.00")
        );

        assertPaid(result, "50.00", "0.00", PaymentMethod.CASH);
        assertSessionCompleted(session.getId(), "50.00");
        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getStatus())
                .isEqualTo(DeviceStatus.AVAILABLE);
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void activeSessionWithProductsCombinesAmountsAndReturnsCashChange() {
        Device device = saveDevice("TEST-PS4-2", DeviceType.PS4);
        GameSession session = saveHourlySession(device, null, 60, START);
        CafeOrder order = saveOrder(session, "Burger", "20.00", 2);

        CheckoutResult result = checkoutService.checkout(
                session.getId(),
                PaymentMethod.CASH,
                money("100.00")
        );

        assertThat(result.gamingAmount()).isEqualByComparingTo("50.00");
        assertThat(result.orderAmount()).isEqualByComparingTo("40.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("90.00");
        assertThat(result.changeAmount()).isEqualByComparingTo("10.00");
        assertThat(result.status()).isEqualTo(BillStatus.PAID);

        CafeOrder completedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completedOrder.getTotalAmount()).isEqualByComparingTo("40.00");
        assertSessionCompleted(session.getId(), "50.00");
    }

    @Test
    void cardPaymentWithExactAmountSucceedsWithoutChange() {
        Device device = saveDevice("TEST-PS5-CARD", DeviceType.PS5);
        GameSession session = saveHourlySession(device, null, 60, START);

        CheckoutResult result = checkoutService.checkout(
                session.getId(),
                PaymentMethod.CARD,
                money("50.00")
        );

        assertPaid(result, "50.00", "0.00", PaymentMethod.CARD);
    }

    @Test
    void mobileWalletPaymentWithExactAmountSucceedsWithoutChange() {
        Device device = saveDevice("TEST-PS5-WALLET", DeviceType.PS5);
        GameSession session = saveHourlySession(device, null, 60, START);

        CheckoutResult result = checkoutService.checkout(
                session.getId(),
                PaymentMethod.MOBILE_WALLET,
                money("50.00")
        );

        assertPaid(result, "50.00", "0.00", PaymentMethod.MOBILE_WALLET);
    }

    @Test
    void cashBelowTotalLeavesSessionAndOrderUnchanged() {
        Device device = saveDevice("TEST-PS4-UNDER", DeviceType.PS4);
        GameSession session = saveHourlySession(device, null, 60, START);
        CafeOrder order = saveOrder(session, "Pizza", "20.00", 2);

        assertThatThrownBy(() -> checkoutService.checkout(
                session.getId(),
                PaymentMethod.CASH,
                money("89.99")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount tendered is less than the bill total");

        assertUnchangedAfterFailedCheckout(session.getId(), order.getId(), device.getId());
    }

    @Test
    void cardAndMobileWalletIncorrectAmountsRecordNoPayment() {
        List<PaymentMethod> methods = List.of(
                PaymentMethod.CARD,
                PaymentMethod.MOBILE_WALLET
        );

        for (PaymentMethod method : methods) {
            Device device = saveDevice("TEST-INCORRECT-" + method, DeviceType.PS4);
            GameSession session = saveHourlySession(device, null, 60, START);

            assertThatThrownBy(() -> checkoutService.checkout(
                    session.getId(),
                    method,
                    money("49.99")
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Card and mobile-wallet payments must equal the bill total");

            assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus())
                    .isEqualTo(SessionStatus.ACTIVE);
            assertThat(paymentRepository.count()).isZero();
            assertThat(billRepository.count()).isZero();
        }
    }

    @Test
    void matchSessionUsesUnitPriceTimesPurchasedMatches() {
        Device device = saveDevice("TEST-MATCH", DeviceType.PS4);
        GameSession session = GameSession.builder()
                .device(device)
                .startTime(START)
                .plannedMinutes(null)
                .hourlyRateSnapshot(HOURLY_RATE)
                .sessionType(SessionType.MATCH)
                .billingUnit(BillingUnit.MATCH)
                .unitPriceSnapshot(new BigDecimal("25.00"))
                .purchasedMatches(3)
                .completedMatches(0)
                .matchDurationMinutesSnapshot(15)
                .warningBeforeExpiryMinutesSnapshot(2)
                .matchExpired(false)
                .status(SessionStatus.ACTIVE)
                .build();
        session = sessionRepository.save(session);

        CheckoutResult result = checkoutService.checkout(
                session.getId(),
                PaymentMethod.CASH,
                money("75.00")
        );

        assertThat(result.gamingAmount()).isEqualByComparingTo("75.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("75.00");
        assertSessionCompleted(session.getId(), "75.00");
    }

    @Test
    void plannedSessionStopsBillingAtPlannedDuration() {
        Device device = saveDevice("TEST-PLANNED", DeviceType.PS4);
        GameSession session = saveHourlySession(device, null, 30, START);

        CheckoutResult result = checkoutService.checkout(
                session.getId(),
                PaymentMethod.CASH,
                money("25.00")
        );

        assertThat(result.gamingAmount()).isEqualByComparingTo("25.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("25.00");
        assertSessionCompleted(session.getId(), "25.00");
    }

    @Test
    void completedSessionWithPendingBillIsPaidWithoutSecondCompletion() {
        Device device = saveDevice("TEST-PENDING", DeviceType.PS4);
        device.setStatus(DeviceStatus.AVAILABLE);
        deviceRepository.save(device);

        LocalDateTime endTime = START.plusHours(1);
        GameSession session = GameSession.builder()
                .device(device)
                .startTime(START)
                .endTime(endTime)
                .plannedMinutes(60)
                .hourlyRateSnapshot(HOURLY_RATE)
                .sessionType(SessionType.SINGLE)
                .billingUnit(BillingUnit.HOUR)
                .unitPriceSnapshot(HOURLY_RATE)
                .finalAmount(money("50.00"))
                .status(SessionStatus.COMPLETED)
                .build();
        session = sessionRepository.save(session);

        Bill bill = billRepository.save(Bill.builder()
                .billNumber(uniqueBillNumber())
                .session(session)
                .gamingAmount(money("50.00"))
                .orderAmount(money("0.00"))
                .totalAmount(money("50.00"))
                .status(BillStatus.PENDING_PAYMENT)
                .createdAt(endTime)
                .build());

        CheckoutResult result = checkoutService.checkout(
                session.getId(),
                PaymentMethod.CASH,
                money("50.00")
        );

        assertThat(result.billId()).isEqualTo(bill.getId());
        assertThat(result.status()).isEqualTo(BillStatus.PAID);
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getEndTime())
                .isEqualTo(endTime);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(billRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidSessionIdProducesClearErrorWithoutDatabaseChanges() {
        assertThatThrownBy(() -> checkoutService.checkout(
                999999L,
                PaymentMethod.CASH,
                money("50.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Session not found");

        assertThat(sessionRepository.count()).isZero();
        assertThat(orderRepository.count()).isZero();
        assertThat(billRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void repeatCheckoutReturnsExistingPaymentWithoutCreatingDuplicate() {
        Device device = saveDevice("TEST-REPEAT", DeviceType.PS4);
        GameSession session = saveHourlySession(device, null, 60, START);

        CheckoutResult first = checkoutService.checkout(
                session.getId(),
                PaymentMethod.CASH,
                money("50.00")
        );
        CheckoutResult second = checkoutService.checkout(
                session.getId(),
                PaymentMethod.CASH,
                money("50.00")
        );

        assertThat(second.billId()).isEqualTo(first.billId());
        assertThat(second.status()).isEqualTo(BillStatus.PAID);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(billRepository.count()).isEqualTo(1);
    }

    @Test
    void missingPaymentMethodFailsWithoutChangesAndMissingAmountMeansExactPayment() {
        Device device = saveDevice("TEST-VALIDATION", DeviceType.PS4);
        GameSession session = saveHourlySession(device, null, 60, START);

        assertThatThrownBy(() -> checkoutService.checkout(
                session.getId(),
                null,
                money("50.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment method is required");

        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.ACTIVE);
        assertThat(billRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();

        CheckoutResult result = checkoutService.checkout(
                session.getId(),
                PaymentMethod.CASH,
                null
        );

        assertPaid(result, "50.00", "0.00", PaymentMethod.CASH);
        assertThat(paymentRepository.findAll()).singleElement()
                .satisfies(payment -> assertThat(payment.getAmountTendered())
                        .isEqualByComparingTo("50.00"));
    }

    private Device saveDevice(String name, DeviceType type) {
        return deviceRepository.save(Device.builder()
                .name(name)
                .type(type)
                .status(DeviceStatus.PLAYING)
                .active(true)
                .build());
    }

    private GameSession saveHourlySession(
            Device device,
            CafeOrder ignoredOrder,
            Integer plannedMinutes,
            LocalDateTime startTime
    ) {
        return sessionRepository.save(GameSession.builder()
                .device(device)
                .startTime(startTime)
                .plannedMinutes(plannedMinutes)
                .hourlyRateSnapshot(HOURLY_RATE)
                .sessionType(SessionType.SINGLE)
                .billingUnit(BillingUnit.HOUR)
                .unitPriceSnapshot(HOURLY_RATE)
                .status(SessionStatus.ACTIVE)
                .build());
    }

    private CafeOrder saveOrder(
            GameSession session,
            String productName,
            String productPrice,
            int quantity
    ) {
        Product product = productRepository.save(Product.builder()
                .name(productName + "-" + UUID.randomUUID())
                .price(money(productPrice))
                .active(true)
                .deleted(false)
                .build());

        CafeOrder order = CafeOrder.builder()
                .gameSession(session)
                .createdAt(START)
                .status(OrderStatus.OPEN)
                .totalAmount(money("0.00"))
                .build();
        order.addItem(OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPriceSnapshot(money(productPrice))
                .lineTotal(money("0.00"))
                .build());
        return orderRepository.save(order);
    }

    private void assertPaid(
            CheckoutResult result,
            String total,
            String change,
            PaymentMethod method
    ) {
        assertThat(result.status()).isEqualTo(BillStatus.PAID);
        assertThat(result.totalAmount()).isEqualByComparingTo(total);
        assertThat(result.paymentMethod()).isEqualTo(method);
        assertThat(result.changeAmount()).isEqualByComparingTo(change);
    }

    private void assertSessionCompleted(Long sessionId, String finalAmount) {
        GameSession saved = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(saved.getFinalAmount()).isEqualByComparingTo(finalAmount);
        assertThat(saved.getEndTime()).isNotNull();
    }

    private void assertUnchangedAfterFailedCheckout(
            Long sessionId,
            Long orderId,
            Long deviceId
    ) {
        assertThat(sessionRepository.findById(sessionId).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.ACTIVE);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.OPEN);
        assertThat(deviceRepository.findById(deviceId).orElseThrow().getStatus())
                .isEqualTo(DeviceStatus.PLAYING);
        assertThat(billRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();
    }

    private String uniqueBillNumber() {
        return "BILL-TEST-" + UUID.randomUUID();
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
