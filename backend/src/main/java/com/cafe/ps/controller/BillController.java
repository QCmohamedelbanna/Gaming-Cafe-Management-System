package com.cafe.ps.controller;

import com.cafe.ps.dto.CheckoutResult;
import com.cafe.ps.dto.CheckoutRequest;
import com.cafe.ps.dto.RefundRequest;
import com.cafe.ps.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.cafe.ps.entity.PaymentMethod;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERMISSION_CHECKOUT_USE')")
public class BillController {

    private final BillingService billingService;

    @GetMapping("/pending")
    public List<CheckoutResult> pending() {
        return billingService.getPendingBills();
    }

    @GetMapping("/alerts")
    public List<CheckoutResult> alerts() {
        return billingService.getActiveExpiryAlerts();
    }

    @GetMapping("/{id}")
    public CheckoutResult get(@PathVariable Long id) {
        return billingService.getBill(id);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('PERMISSION_BILL_REFUND')")
    public CheckoutResult refund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request
    ) {
        return billingService.refund(id, request.reason());
    }

    @PostMapping("/{id}/pay")
    public CheckoutResult pay(
            @PathVariable Long id,
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication
    ) {
        return billingService.payBill(
                id,
                request.paymentMethod(),
                request.amountTendered(),
                authentication.getName()
        );
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERMISSION_BILLING_MANAGE')")
    public CheckoutResult cancel(@PathVariable Long id) {
        return billingService.cancelBill(id);
    }
}
