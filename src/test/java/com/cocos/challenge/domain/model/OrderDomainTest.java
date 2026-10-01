package com.cocos.challenge.domain.model;

import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.exception.InvalidOrderStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderDomainTest {

    @Test
    @DisplayName("Market order is created with FILLED status immediately")
    void testCreateMarketOrder() {
        Order order = Order.builder()
                .userId(1L)
                .instrumentId(10L)
                .size(50)
                .price(BigDecimal.valueOf(100.50))
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .status(OrderStatus.FILLED)
                .datetime(LocalDateTime.now())
                .idempotencyKey("key-123")
                .build();

        assertEquals(OrderStatus.FILLED, order.getStatus());
        assertEquals(OrderType.MARKET, order.getType());
        assertEquals(50, order.getSize());
        assertEquals(BigDecimal.valueOf(100.50), order.getPrice());
        assertEquals(BigDecimal.valueOf(5025.00).setScale(2), order.totalValue());
        assertNotNull(order.getDatetime());
    }

    @Test
    @DisplayName("Limit order is created with NEW status")
    void testCreateLimitOrder() {
        Order order = Order.builder()
                .userId(1L)
                .instrumentId(10L)
                .size(20)
                .price(BigDecimal.valueOf(150.00))
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .status(OrderStatus.NEW)
                .datetime(LocalDateTime.now())
                .idempotencyKey("key-456")
                .build();

        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(OrderType.LIMIT, order.getType());
        assertEquals(20, order.getSize());
    }

    @Test
    @DisplayName("Only NEW order can be cancelled; FILLED order throws InvalidOrderStateException")
    void testCancelOrderRules() {
        Order limitOrder = Order.builder()
                .userId(1L)
                .instrumentId(10L)
                .size(20)
                .price(BigDecimal.valueOf(150.00))
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .status(OrderStatus.NEW)
                .build();
        assertEquals(OrderStatus.NEW, limitOrder.getStatus());

        limitOrder.cancel();
        assertEquals(OrderStatus.CANCELLED, limitOrder.getStatus());

        // Attempting to cancel already cancelled order
        assertThrows(InvalidOrderStateException.class, limitOrder::cancel);

        // Attempting to cancel filled market order
        Order marketOrder = Order.builder()
                .userId(1L)
                .instrumentId(10L)
                .size(50)
                .price(BigDecimal.valueOf(100.50))
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .status(OrderStatus.FILLED)
                .build();
        assertThrows(InvalidOrderStateException.class, marketOrder::cancel);
    }

    @Test
    @DisplayName("Rejected order has REJECTED status and cannot be cancelled")
    void testCreateRejectedOrder() {
        Order order = Order.builder()
                .userId(1L)
                .instrumentId(10L)
                .size(100)
                .price(BigDecimal.valueOf(50.00))
                .type(OrderType.MARKET)
                .side(OrderSide.BUY)
                .status(OrderStatus.REJECTED)
                .idempotencyKey("key-rej")
                .rejectionReason("Insufficient funds")
                .build();
        assertEquals(OrderStatus.REJECTED, order.getStatus());
        assertEquals("Insufficient funds", order.getRejectionReason());
        assertThrows(InvalidOrderStateException.class, order::cancel);
    }

    @Test
    @DisplayName("Only NEW orders can be rejected; non-NEW or blank reason throws exception")
    void testRejectOrderRules() {
        Order newOrder = Order.builder()
                .userId(1L)
                .instrumentId(10L)
                .size(10)
                .price(BigDecimal.valueOf(100.00))
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .status(OrderStatus.NEW)
                .build();

        // Blank rejection reason fails
        assertThrows(IllegalArgumentException.class, () -> newOrder.reject("   "));
        assertThrows(IllegalArgumentException.class, () -> newOrder.reject(null));

        // Rejecting NEW order succeeds
        newOrder.reject("Price slipped beyond limit");
        assertEquals(OrderStatus.REJECTED, newOrder.getStatus());
        assertEquals("Price slipped beyond limit", newOrder.getRejectionReason());

        // Rejecting an already rejected order fails
        assertThrows(InvalidOrderStateException.class, () -> newOrder.reject("Another reason"));

        // Rejecting a FILLED order fails
        Order filledOrder = Order.builder()
                .userId(1L)
                .instrumentId(10L)
                .size(10)
                .price(BigDecimal.valueOf(100.00))
                .type(OrderType.MARKET)
                .side(OrderSide.BUY)
                .status(OrderStatus.FILLED)
                .build();
        assertThrows(InvalidOrderStateException.class, () -> filledOrder.reject("Cannot reject filled"));
    }

    @Test
    @DisplayName("isFilled query returns true only when status is FILLED")
    void testIsFilledQuery() {
        Order filled = Order.builder().status(OrderStatus.FILLED).build();
        org.junit.jupiter.api.Assertions.assertTrue(filled.isFilled());

        Order newOrder = Order.builder().status(OrderStatus.NEW).build();
        org.junit.jupiter.api.Assertions.assertFalse(newOrder.isFilled());
    }

    @Test
    @DisplayName("totalValue falls back to BigDecimal.ONE when price is null (cash movement)")
    void testTotalValueEffectivePrice() {
        Order cashOrder = Order.builder()
                .size(5000)
                .price(null) // cash movement with unit price fallback
                .build();
        assertEquals(BigDecimal.valueOf(5000.00).setScale(2), cashOrder.totalValue());
    }
}
