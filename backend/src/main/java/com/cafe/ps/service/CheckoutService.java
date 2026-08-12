package com.cafe.ps.service;

import com.cafe.ps.dto.CheckoutResult;
import com.cafe.ps.entity.Bill;
import com.cafe.ps.entity.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final BillingService billingService;

    @Transactional
    public CheckoutResult prepareCheckout(Long sessionId) {
        Bill bill = billingService.finalizeSession(
                sessionId,
                LocalDateTime.now()
        );

        return billingService.getBill(bill.getId());
    }

    public CheckoutResult checkout(
            Long sessionId,
            PaymentMethod method,
            BigDecimal amountTendered
    ) {
        return billingService.checkoutSession(
                sessionId,
                method,
                amountTendered
        );
    }
}
