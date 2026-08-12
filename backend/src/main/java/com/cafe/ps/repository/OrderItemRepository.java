package com.cafe.ps.repository;

import com.cafe.ps.entity.OrderItem;
import com.cafe.ps.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    boolean existsByProductIdAndOrder_Status(
            Long productId,
            OrderStatus status
    );
}
