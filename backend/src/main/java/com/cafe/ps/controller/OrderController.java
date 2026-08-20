package com.cafe.ps.controller;

import com.cafe.ps.dto.AddOrderItemRequest;
import com.cafe.ps.dto.CafeOrderResponse;
import com.cafe.ps.dto.CheckoutRequest;
import com.cafe.ps.dto.CheckoutResult;
import com.cafe.ps.dto.CreateOrderRequest;
import com.cafe.ps.dto.DiscountRequest;
import com.cafe.ps.dto.UpdateOrderItemQuantityRequest;
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
@PreAuthorize("hasAuthority('PERMISSION_POS_USE')")
public class OrderController {

    private final OrderService orderService;
    private final BillingService billingService;

    @PostMapping
    public CafeOrderResponse create(
            @RequestBody CreateOrderRequest request
    ) {
        return CafeOrderResponse.from(orderService.createOrder(
                request.gameSessionId()
        ));
    }

    @GetMapping("/{id}")
    public CafeOrderResponse get(
            @PathVariable Long id
    ) {
        return CafeOrderResponse.from(orderService.get(id));
    }

    @GetMapping("/session/{sessionId}/open")
    public CafeOrderResponse getOpenOrderForSession(
            @PathVariable Long sessionId
    ) {
        var order = orderService.getOpenOrderForSession(sessionId);
        return order == null ? null : CafeOrderResponse.from(order);
    }

    @PostMapping("/{id}/items")
    public CafeOrderResponse addItem(
            @PathVariable Long id,
            @Valid
            @RequestBody AddOrderItemRequest request
    ) {
        return CafeOrderResponse.from(orderService.addItem(
                id,
                request.productId(),
                request.quantity()
        ));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public CafeOrderResponse removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId
    ) {
        return CafeOrderResponse.from(orderService.removeItem(
                orderId,
                itemId
        ));
    }

    @PatchMapping("/{orderId}/items/{itemId}/quantity")
    public CafeOrderResponse updateItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateOrderItemQuantityRequest request
    ) {
        return CafeOrderResponse.from(orderService.updateItemQuantity(
                orderId,
                itemId,
                request.quantity()
        ));
    }

    @PatchMapping("/{id}/discount")
    @PreAuthorize("hasAuthority('PERMISSION_DISCOUNTS_MANAGE')")
    public CafeOrderResponse applyDiscount(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequest request,
            Authentication authentication
    ) {
        boolean hasDiscountPermission = authentication.getAuthorities().stream()
                .anyMatch(granted -> "PERMISSION_DISCOUNTS_MANAGE".equals(granted.getAuthority()));
        return CafeOrderResponse.from(orderService.applyDiscount(id, request, hasDiscountPermission));
    }

    @DeleteMapping("/{id}/discount")
    public CafeOrderResponse clearDiscount(@PathVariable Long id) {
        return CafeOrderResponse.from(orderService.clearDiscount(id));
    }

    @PostMapping("/{id}/hold")
    public CafeOrderResponse hold(@PathVariable Long id) {
        return CafeOrderResponse.from(orderService.holdOrder(id));
    }

    @PostMapping("/{id}/resume")
    public CafeOrderResponse resume(@PathVariable Long id) {
        return CafeOrderResponse.from(orderService.resumeOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public CafeOrderResponse cancel(@PathVariable Long id) {
        return CafeOrderResponse.from(orderService.cancelOrder(id));
    }

    @GetMapping("/held")
    public java.util.List<CafeOrderResponse> held() {
        return orderService.getHeldOrders().stream().map(CafeOrderResponse::from).toList();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('PERMISSION_CHECKOUT_USE')")
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
