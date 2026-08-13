package com.cafe.ps.service;

import com.cafe.ps.dto.StockMovementRequest;
import com.cafe.ps.entity.BillStatus;
import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.OrderItem;
import com.cafe.ps.entity.OrderStatus;
import com.cafe.ps.entity.Product;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:inventory-tests?mode=memory&cache=shared",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false",
                "spring.datasource.hikari.maximum-pool-size=1",
                "inventory.prevent-negative=true"
        }
)
class InventoryServiceIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CafeOrderRepository orderRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        billRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        sessionRepository.deleteAll();
        movementRepository.deleteAll();
        productRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void purchaseAndAdjustmentCreateLedgerEntriesAndUpdateCachedBalance() {
        Product product = saveTrackedProduct("Cola", "5.00", "1.50");

        Product purchased = inventoryService.purchase(
                product.getId(),
                new StockMovementRequest(
                        new BigDecimal("12"),
                        new BigDecimal("1.25"),
                        "INV-100",
                        "Mona"
                )
        );
        Product adjusted = inventoryService.adjust(
                product.getId(),
                new StockMovementRequest(
                        new BigDecimal("-2"),
                        null,
                        "COUNT-1",
                        "Mona"
                )
        );

        assertThat(purchased.getCurrentStock()).isEqualByComparingTo("12.000");
        assertThat(adjusted.getCurrentStock()).isEqualByComparingTo("10.000");
        assertThat(movementRepository.findAll())
                .extracting("type")
                .containsExactlyInAnyOrder(
                        StockMovementType.PURCHASE,
                        StockMovementType.ADJUSTMENT
                );
        assertThat(movementRepository.findAll())
                .extracting("quantity")
                .containsExactlyInAnyOrder(
                        new BigDecimal("12"),
                        new BigDecimal("-2")
                );
    }

    @Test
    void negativeInventoryIsRejectedWithoutWritingASaleMovement() {
        Product product = saveTrackedProduct("Juice", "10.00", "3.00");
        inventoryService.purchase(product.getId(), new StockMovementRequest(
                new BigDecimal("1"), null, "OPENING", "admin"
        ));
        CafeOrder order = saveStandaloneOrder(product, 2);

        assertThatThrownBy(() -> billingService.checkoutOrder(
                order.getId(),
                com.cafe.ps.entity.PaymentMethod.CASH,
                new BigDecimal("20.00")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");

        assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentStock())
                .isEqualByComparingTo("1.000");
        assertThat(movementRepository.findAll())
                .extracting("type")
                .containsExactly(StockMovementType.PURCHASE);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.OPEN);
        assertThat(billRepository.count()).isZero();
    }

    @Test
    void paidSaleReducesStockAndRefundRestoresIt() {
        Product product = saveTrackedProduct("Sandwich", "20.00", "8.00");
        inventoryService.purchase(product.getId(), new StockMovementRequest(
                new BigDecimal("5"), null, "OPENING", "admin"
        ));
        CafeOrder order = saveStandaloneOrder(product, 2);

        var paid = billingService.checkoutOrder(
                order.getId(),
                com.cafe.ps.entity.PaymentMethod.CASH,
                new BigDecimal("40.00")
        );

        assertThat(paid.status()).isEqualTo(BillStatus.PAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentStock())
                .isEqualByComparingTo("3.000");
        assertThat(movementRepository.findAll())
                .extracting("type")
                .containsExactlyInAnyOrder(
                        StockMovementType.PURCHASE,
                        StockMovementType.SALE
                );

        var refunded = billingService.refund(paid.billId(), "Customer return");

        assertThat(refunded.status()).isEqualTo(BillStatus.REFUNDED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentStock())
                .isEqualByComparingTo("5.000");
        assertThat(movementRepository.findAll())
                .extracting("type")
                .containsExactlyInAnyOrder(
                        StockMovementType.PURCHASE,
                        StockMovementType.SALE,
                        StockMovementType.RETURN
                );
    }

    @Test
    void addingTrackedProductBeyondAvailableStockIsRejected() {
        Product product = saveTrackedProduct("Limited Cola", "5.00", "1.50");
        inventoryService.purchase(product.getId(), new StockMovementRequest(
                new BigDecimal("2"), null, "OPENING", "admin"
        ));
        CafeOrder order = orderService.createOrder(null);

        orderService.addItem(order.getId(), product.getId(), 2);

        assertThatThrownBy(() -> orderService.addItem(
                order.getId(), product.getId(), 1
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock")
                .hasMessageContaining("Available: 2");

        var items = orderItemRepository.findAll();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void deletedProductIsExcludedFromInventoryMovementList() {
        Product product = saveTrackedProduct("Archived Cola", "5.00", "1.50");
        inventoryService.purchase(product.getId(), new StockMovementRequest(
                new BigDecimal("2"), null, "OPENING", "admin"
        ));

        productService.delete(product.getId());

        assertThat(inventoryService.getMovements(null)).isEmpty();
        assertThat(inventoryService.getMovements(product.getId())).isEmpty();
    }

    private Product saveTrackedProduct(String name, String price, String cost) {
        return productRepository.save(Product.builder()
                .name(name)
                .price(new BigDecimal(price))
                .sellingPrice(new BigDecimal(price))
                .costPrice(new BigDecimal(cost))
                .trackStock(true)
                .currentStock(BigDecimal.ZERO)
                .minimumStock(BigDecimal.ONE)
                .category("Food")
                .unit("piece")
                .active(true)
                .deleted(false)
                .build());
    }

    private CafeOrder saveStandaloneOrder(Product product, int quantity) {
        CafeOrder order = CafeOrder.builder()
                .createdAt(java.time.LocalDateTime.now())
                .status(OrderStatus.OPEN)
                .totalAmount(BigDecimal.ZERO)
                .build();
        order.addItem(OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPriceSnapshot(product.getSellingPrice())
                .lineTotal(BigDecimal.ZERO)
                .build());
        return orderRepository.save(order);
    }
}
