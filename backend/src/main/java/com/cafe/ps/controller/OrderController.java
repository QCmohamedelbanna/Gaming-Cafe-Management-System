package com.cafe.ps.controller;

import com.cafe.ps.dto.AddOrderItemRequest;
import com.cafe.ps.dto.CheckoutRequest;
import com.cafe.ps.dto.CheckoutResult;
import com.cafe.ps.dto.CreateOrderRequest;
import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.PaymentMethod;
import com.cafe.ps.service.BillingService;
import com.cafe.ps.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    private final OrderService orderService;
    private final BillingService billingService;

    @PostMapping
    public CafeOrder create(
            @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(
                request.gameSessionId()
        );
    }

    @GetMapping("/{id}")
    public CafeOrder get(
            @PathVariable Long id
    ) {
        return orderService.get(id);
    }

    @GetMapping("/session/{sessionId}/open")
    public CafeOrder getOpenOrderForSession(
            @PathVariable Long sessionId
    ) {
        return orderService.getOpenOrderForSession(sessionId);
    }

    @PostMapping("/{id}/items")
    public CafeOrder addItem(
            @PathVariable Long id,
            @Valid
            @RequestBody AddOrderItemRequest request
    ) {
        return orderService.addItem(
                id,
                request.productId(),
                request.quantity()
        );
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public CafeOrder removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId
    ) {
        return orderService.removeItem(
                orderId,
                itemId
        );
    }

    @PostMapping("/{id}/complete")
    public CheckoutResult complete(
            @PathVariable Long id,
            @RequestBody(required = false) CheckoutRequest request
    ) {
        return billingService.checkoutOrder(
                id,
                request == null ? PaymentMethod.CASH : request.paymentMethod(),
                request == null ? null : request.amountTendered()
        );
    }
}
