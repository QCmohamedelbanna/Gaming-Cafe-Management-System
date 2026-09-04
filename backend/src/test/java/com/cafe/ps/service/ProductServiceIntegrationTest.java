package com.cafe.ps.service;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import com.cafe.ps.dto.ProductRequest;
import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.OrderItem;
import com.cafe.ps.entity.OrderStatus;
import com.cafe.ps.entity.Product;
import com.cafe.ps.repository.CafeOrderRepository;
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
class ProductServiceIntegrationTest extends AbstractMySQLIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CafeOrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @BeforeEach
    void cleanDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void deletingAProductReferencedByAnOpenOrderIsRejected() {
        Product product = saveProduct("Open Order Cola");
        attachToOrder(product, OrderStatus.OPEN);

        assertThatThrownBy(() -> productService.delete(product.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("open order");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getDeleted()).isFalse();
    }

    @Test
    void deletingAProductIsAllowedOnceItsOrderIsNoLongerOpen() {
        Product product = saveProduct("Completed Order Cola");
        attachToOrder(product, OrderStatus.COMPLETED);

        productService.delete(product.getId());

        Product deleted = productRepository.findById(product.getId()).orElseThrow();
        assertThat(deleted.getDeleted()).isTrue();
        assertThat(deleted.getActive()).isFalse();
    }

    @Test
    void deleteIsSoftAndProductDisappearsFromActiveAndAllListings() {
        Product product = saveProduct("Soft Delete Cola");

        productService.delete(product.getId());

        assertThat(productRepository.findById(product.getId())).isPresent();
        assertThat(productService.getActiveProducts()).noneMatch(p -> p.getId().equals(product.getId()));
        assertThat(productService.getAllProducts()).noneMatch(p -> p.getId().equals(product.getId()));
    }

    @Test
    void creatingAProductWithADuplicateNameIsRejected() {
        Product existing = saveProduct("Duplicate Cola");

        ProductRequest request = new ProductRequest(
                existing.getName(), new BigDecimal("5.00"), null, null,
                null, null, null, null, null
        );

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void creatingAProductWithADuplicateSkuIsRejected() {
        Product existing = saveProduct("SKU Cola");
        existing.setSku("SKU-100");
        productRepository.save(existing);

        ProductRequest request = new ProductRequest(
                "Another Cola", new BigDecimal("5.00"), null, "SKU-100",
                null, null, null, null, null
        );

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    void creatingAProductWithNonPositiveSellingPriceIsRejected() {
        ProductRequest request = new ProductRequest(
                "Zero Price Cola", BigDecimal.ZERO, null, null,
                null, null, null, null, null
        );

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    private Product saveProduct(String name) {
        return productRepository.save(Product.builder()
                .name(name + "-" + UUID.randomUUID())
                .price(new BigDecimal("5.00"))
                .sellingPrice(new BigDecimal("5.00"))
                .active(true)
                .deleted(false)
                .build());
    }

    private void attachToOrder(Product product, OrderStatus status) {
        CafeOrder order = CafeOrder.builder()
                .createdAt(LocalDateTime.now())
                .status(status)
                .totalAmount(new BigDecimal("5.00"))
                .build();
        order.addItem(OrderItem.builder()
                .product(product)
                .quantity(1)
                .unitPriceSnapshot(new BigDecimal("5.00"))
                .lineTotal(new BigDecimal("5.00"))
                .build());
        orderRepository.save(order);
    }
}
