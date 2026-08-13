package com.cafe.ps.repository;

import com.cafe.ps.entity.StockMovement;
import com.cafe.ps.entity.StockMovementType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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

    boolean existsByProductIdAndTypeAndReference(
            Long productId,
            StockMovementType type,
            String reference
    );
}
