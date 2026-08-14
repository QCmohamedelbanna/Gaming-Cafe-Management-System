package com.cafe.ps.repository;

import com.cafe.ps.entity.Shift;
import com.cafe.ps.entity.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findByUserIdAndStatus(Long userId, ShiftStatus status);

    List<Shift> findByUserIdOrderByOpenedAtDesc(Long userId);

    List<Shift> findAllByOrderByOpenedAtDesc();
}
