package com.cafe.ps.controller;

import com.cafe.ps.dto.AddOrderItemRequest;
import com.cafe.ps.dto.CreateOrderRequest;
import com.cafe.ps.entity.CafeOrder;
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
    public CafeOrder complete(
            @PathVariable Long id
    ) {
        return orderService.completeOrder(id);
    }
}