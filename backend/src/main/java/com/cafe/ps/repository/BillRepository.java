package com.cafe.ps.repository;

import com.cafe.ps.entity.Bill;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import com.cafe.ps.entity.BillStatus;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findBySessionId(Long sessionId);

    Optional<Bill> findByOrderId(Long orderId);

    /**
     * Locks the bill row so two concurrent payment attempts on the same
     * pending bill serialize instead of both passing the PAID-status guard
     * in BillingService#settleBill and recording two payments.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") Long id);

    List<Bill> findByPaidAtBetweenAndStatus(
            LocalDateTime from,
            LocalDateTime to,
            BillStatus status
    );

    @EntityGraph(attributePaths = {
            "session",
            "session.device",
            "order",
            "order.items",
            "order.items.product"
    })
    List<Bill> findByStatusAndPaidAtBetweenOrderByPaidAtAsc(
            BillStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    @EntityGraph(attributePaths = {
            "session",
            "session.device",
            "order",
            "order.items",
            "order.items.product"
    })
    List<Bill> findByStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
            BillStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    @EntityGraph(attributePaths = {
            "session",
            "session.device",
            "order",
            "order.items",
            "order.items.product"
    })
    List<Bill> findByStatusAndRefundedAtBetweenOrderByRefundedAtAsc(
            BillStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    @EntityGraph(attributePaths = {
            "session",
            "session.device",
            "order",
            "order.items",
            "order.items.product"
    })
    List<Bill> findByStatusOrderByCreatedAtDesc(BillStatus status);

    @EntityGraph(attributePaths = {
            "session",
            "session.device",
            "order",
            "order.items",
            "order.items.product"
    })
    List<Bill> findByStatusAndAutomaticExpiryTrueAndNotificationExpiresAtAfterOrderByCreatedAtDesc(
            BillStatus status,
            LocalDateTime now
    );

    @EntityGraph(attributePaths = {
            "session",
            "session.device",
            "order",
            "order.items",
            "order.items.product"
    })
    @Query("select distinct b from Bill b where b.id = :id")
    Optional<Bill> findDetailedById(@Param("id") Long id);
}
