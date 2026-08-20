package com.cafe.ps.service;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import com.cafe.ps.entity.BillingUnit;
import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.OrderStatus;
import com.cafe.ps.entity.Product;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.repository.CafeOrderRepository;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import com.cafe.ps.repository.OrderItemRepository;
import com.cafe.ps.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false"
        }
)
class OrderServiceIntegrationTest extends AbstractMySQLIntegrationTest {

    @Autowired
    private OrderService orderService;

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

    @BeforeEach
    void cleanDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        sessionRepository.deleteAll();
        deviceRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void secondCreateOrderForTheSameSessionReturnsTheExistingOpenOrder() {
        GameSession session = saveActiveSession();

        CafeOrder first = orderService.createOrder(session.getId());
        CafeOrder second = orderService.createOrder(session.getId());

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void createOrderForAnInactiveSessionFails() {
        GameSession session = saveActiveSession();
        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);

        assertThatThrownBy(() -> orderService.createOrder(session.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active session");
    }

    @Test
    void standaloneOrdersAreNotDeduplicated() {
        CafeOrder first = orderService.createOrder(null);
        CafeOrder second = orderService.createOrder(null);

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void standaloneOrderCanBeHeldResumedAndCancelled() {
        Product product = saveProduct();
        CafeOrder order = orderService.createOrder(null);
        orderService.addItem(order.getId(), product.getId(), 1);

        CafeOrder held = orderService.holdOrder(order.getId());
        assertThat(held.getStatus()).isEqualTo(OrderStatus.HELD);

        CafeOrder resumed = orderService.resumeOrder(order.getId());
        assertThat(resumed.getStatus()).isEqualTo(OrderStatus.OPEN);

        CafeOrder cancelled = orderService.cancelOrder(order.getId());
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void sessionAttachedOrderCannotBeHeldOrCancelledFromPOS() {
        GameSession session = saveActiveSession();
        Product product = saveProduct();
        CafeOrder order = orderService.createOrder(session.getId());
        orderService.addItem(order.getId(), product.getId(), 1);

        assertThatThrownBy(() -> orderService.holdOrder(order.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("session checkout");
        assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelled from POS");
    }

    @Test
    void emptyStaleStandaloneOrderIsCancelledBySweepButRecentOneIsNot() {
        CafeOrder stale = orderService.createOrder(null);
        backdateCreatedAt(stale, LocalDateTime.now().minusMinutes(10));
        CafeOrder recent = orderService.createOrder(null);

        orderService.cancelStaleEmptyStandaloneOrders();

        assertThat(orderRepository.findById(stale.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(orderRepository.findById(recent.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.OPEN);
    }

    @Test
    void staleStandaloneOrderWithItemsIsNotSweptEvenThoughItIsOld() {
        Product product = saveProduct();
        CafeOrder stale = orderService.createOrder(null);
        orderService.addItem(stale.getId(), product.getId(), 1);
        backdateCreatedAt(stale, LocalDateTime.now().minusMinutes(10));

        orderService.cancelStaleEmptyStandaloneOrders();

        assertThat(orderRepository.findById(stale.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.OPEN);
    }

    @Test
    void removingTheLastItemCancelsAnEmptyStandaloneOrderButNotASessionAttachedOne() {
        Product product = saveProduct();

        CafeOrder standalone = orderService.createOrder(null);
        CafeOrder withItem = orderService.addItem(standalone.getId(), product.getId(), 1);
        Long standaloneItemId = withItem.getItems().get(0).getId();
        CafeOrder afterRemoval = orderService.removeItem(standalone.getId(), standaloneItemId);
        assertThat(afterRemoval.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        GameSession session = saveActiveSession();
        CafeOrder attached = orderService.createOrder(session.getId());
        CafeOrder attachedWithItem = orderService.addItem(attached.getId(), product.getId(), 1);
        Long attachedItemId = attachedWithItem.getItems().get(0).getId();
        CafeOrder attachedAfterRemoval = orderService.removeItem(attached.getId(), attachedItemId);
        assertThat(attachedAfterRemoval.getStatus()).isEqualTo(OrderStatus.OPEN);
    }

    private GameSession saveActiveSession() {
        Device device = deviceRepository.save(Device.builder()
                .name("ORDER-TEST-" + UUID.randomUUID())
                .type(DeviceType.PS4)
                .status(DeviceStatus.PLAYING)
                .active(true)
                .deleted(false)
                .build());
        return sessionRepository.save(GameSession.builder()
                .device(device)
                .startTime(LocalDateTime.now())
                .hourlyRateSnapshot(new BigDecimal("40.00"))
                .unitPriceSnapshot(new BigDecimal("40.00"))
                .sessionType(SessionType.SINGLE)
                .billingUnit(BillingUnit.HOUR)
                .status(SessionStatus.ACTIVE)
                .build());
    }

    private Product saveProduct() {
        return productRepository.save(Product.builder()
                .name("Order Test Product " + UUID.randomUUID())
                .price(new BigDecimal("5.00"))
                .sellingPrice(new BigDecimal("5.00"))
                .active(true)
                .deleted(false)
                .build());
    }

    private void backdateCreatedAt(CafeOrder order, LocalDateTime createdAt) {
        CafeOrder managed = orderRepository.findById(order.getId()).orElseThrow();
        managed.setCreatedAt(createdAt);
        orderRepository.save(managed);
    }
}
