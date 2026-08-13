package com.cafe.ps.repository;

import com.cafe.ps.entity.StockMovement;
import com.cafe.ps.entity.StockMovementType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @EntityGraph(attributePaths = "product")
    List<StockMovement> findTop100ByProduct_DeletedFalseOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "product")
    List<StockMovement> findTop100ByProductIdAndProduct_DeletedFalseOrderByCreatedAtDesc(
            Long productId
    );

    List<StockMovement> findByTypeAndReferenceOrderByCreatedAtAsc(
            StockMovementType type,
            String reference
    );

    @EntityGraph(attributePaths = "product")
    List<StockMovement> findByTypeAndReferenceIn(
            StockMovementType type,
            Collection<String> references
    );

    boolean existsByProductIdAndTypeAndReference(
            Long productId,
            StockMovementType type,
            String reference
    );
}
