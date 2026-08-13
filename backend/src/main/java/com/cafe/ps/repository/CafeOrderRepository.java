package com.cafe.ps.repository;

import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.OrderStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface CafeOrderRepository
        extends JpaRepository<CafeOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    Optional<CafeOrder> findById(Long id);

    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    Optional<CafeOrder>
    findFirstByGameSessionIdAndStatusOrderByCreatedAtDesc(
            Long gameSessionId,
            OrderStatus status
    );

    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    List<CafeOrder> findByGameSessionIsNullAndStatusOrderByCreatedAtDesc(
            OrderStatus status
    );

    @Query("""
            select distinct o from CafeOrder o
            left join fetch o.items i
            where o.status = :status
              and o.gameSession is null
              and o.createdAt < :before
            """)
    List<CafeOrder> findStaleEmptyStandaloneOrders(
            @Param("status") OrderStatus status,
            @Param("before") LocalDateTime before
    );
}
