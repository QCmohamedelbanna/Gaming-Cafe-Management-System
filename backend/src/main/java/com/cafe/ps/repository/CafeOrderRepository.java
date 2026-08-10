package com.cafe.ps.repository;

import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CafeOrderRepository
        extends JpaRepository<CafeOrder, Long> {

    Optional<CafeOrder>
    findFirstByGameSessionIdAndStatusOrderByCreatedAtDesc(
            Long gameSessionId,
            OrderStatus status
    );
}