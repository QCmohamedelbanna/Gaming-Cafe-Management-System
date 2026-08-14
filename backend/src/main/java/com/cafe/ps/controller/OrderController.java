package com.cafe.ps.controller;

import com.cafe.ps.dto.AddOrderItemRequest;
import com.cafe.ps.dto.CheckoutRequest;
import com.cafe.ps.dto.CheckoutResult;
import com.cafe.ps.dto.CreateOrderRequest;
import com.cafe.ps.dto.DiscountRequest;
import com.cafe.ps.dto.UpdateOrderItemQuantityRequest;
import com.cafe.ps.entity.CafeOrder;
import com.cafe.ps.entity.PaymentMethod;
import com.cafe.ps.service.BillingService;
import com.cafe.ps.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'ADMIN')")
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

    @PatchMapping("/{orderId}/items/{itemId}/quantity")
    public CafeOrder updateItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateOrderItemQuantityRequest request
    ) {
        return orderService.updateItemQuantity(
                orderId,
                itemId,
                request.quantity()
        );
    }

    @PatchMapping("/{id}/discount")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public CafeOrder applyDiscount(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequest request,
            Authentication authentication
    ) {
        String userRole = authentication.getAuthorities().stream()
                .map(granted -> granted.getAuthority())
                .filter(value -> value.startsWith("ROLE_"))
                .map(value -> value.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
        return orderService.applyDiscount(id, request, userRole);
    }

    @DeleteMapping("/{id}/discount")
    public CafeOrder clearDiscount(@PathVariable Long id) {
        return orderService.clearDiscount(id);
    }

    @PostMapping("/{id}/hold")
    public CafeOrder hold(@PathVariable Long id) {
        return orderService.holdOrder(id);
    }

    @PostMapping("/{id}/resume")
    public CafeOrder resume(@PathVariable Long id) {
        return orderService.resumeOrder(id);
    }

    @PostMapping("/{id}/cancel")
    public CafeOrder cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @GetMapping("/held")
    public java.util.List<CafeOrder> held() {
        return orderService.getHeldOrders();
    }

    @PostMapping("/{id}/complete")
    public CheckoutResult complete(
            @PathVariable Long id,
            @RequestBody(required = false) CheckoutRequest request,
            Authentication authentication
    ) {
        return billingService.checkoutOrder(
                id,
                request == null ? PaymentMethod.CASH : request.paymentMethod(),
                request == null ? null : request.amountTendered(),
                authentication.getName()
        );
    }
}
