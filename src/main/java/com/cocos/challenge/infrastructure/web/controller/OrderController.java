package com.cocos.challenge.infrastructure.web.controller;

import com.cocos.challenge.application.dto.order.OrderResponse;
import com.cocos.challenge.application.dto.order.SendOrderRequest;
import com.cocos.challenge.application.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Submit an order to the market: MARKET or LIMIT, by size (quantity) or amount ($ ARS).
     * Supports Idempotency-Key header.
     * POST /api/v1/orders
     */
    @PostMapping
    public ResponseEntity<OrderResponse> sendOrder(
            @Valid @RequestBody SendOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        var response = orderService.sendOrder(request, idempotencyKey);

        if (response.isRejected()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancel an order in NEW status.
     * POST /api/v1/orders/{orderId}/cancel
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable("orderId") Long orderId) {
        var response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }
}
