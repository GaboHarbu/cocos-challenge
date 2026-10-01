package com.cocos.challenge.application.dto.order;

import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record OrderResponse(
        Long id,
        Long userId,
        Long instrumentId,
        String ticker,
        String instrumentName,
        OrderSide side,
        OrderType type,
        Integer size,
        BigDecimal price,
        BigDecimal totalValue,
        OrderStatus status,
        LocalDateTime datetime,
        String idempotencyKey,
        String rejectionReason,
        String message
) {
    public Long orderId() {
        return id;
    }

    public boolean isRejected() {
        return status == OrderStatus.REJECTED;
    }
}
