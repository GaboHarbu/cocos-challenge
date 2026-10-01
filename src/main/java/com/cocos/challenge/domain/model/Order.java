package com.cocos.challenge.domain.model;

import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.exception.InvalidOrderStateException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    private Long id;
    private final Long instrumentId;
    private final Long userId;
    private final Integer size;
    private final BigDecimal price;
    private final OrderType type;
    private final OrderSide side;
    private OrderStatus status;

    @Builder.Default
    private final LocalDateTime datetime = LocalDateTime.now();
    private String idempotencyKey;
    private String rejectionReason;

    public boolean isNew() {
        return this.status == OrderStatus.NEW;
    }

    public boolean isFilled() {
        return this.status == OrderStatus.FILLED;
    }

    /**
     * Finite State Machine: Only orders in status NEW can be cancelled.
     */
    public void cancel() {
        if (!isNew()) {
            throw new InvalidOrderStateException(
                    "Only orders in status NEW can be cancelled. Current status is: " + this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * Finite State Machine: Fill a NEW order.
     */
    public void fill() {
        if (!isNew()) {
            throw new InvalidOrderStateException(
                    "Only orders in status NEW can be filled. Current status is: " + this.status);
        }
        this.status = OrderStatus.FILLED;
    }

    /**
     * Rejects the order with a descriptive reason. Only NEW orders can be rejected.
     */
    public void reject(String reason) {
        if (!isNew()) {
            throw new InvalidOrderStateException(
                    "Only orders in status NEW can be rejected. Current status is: " + this.status);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be blank.");
        }
        this.status = OrderStatus.REJECTED;
        this.rejectionReason = reason;
    }

    /**
     * Calculates total order value with banker's rounding (price * size).
     */
    public BigDecimal totalValue() {
        var effectivePrice = price != null ? price : BigDecimal.ONE;
        var effectiveSize = size != null ? size : 0;
        return com.cocos.challenge.domain.util.MoneyUtils.multiply(effectivePrice, effectiveSize);
    }
}
