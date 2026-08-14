package com.cafe.ps.repository;

import com.cafe.ps.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByShiftIdOrderByPaidAtAsc(Long shiftId);
}
