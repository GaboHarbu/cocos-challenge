package com.cocos.challenge.application.mapper;

import com.cocos.challenge.application.dto.order.OrderResponse;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private static final String UNKNOWN_INSTRUMENT = "N/A";

    public OrderResponse toResponse(OrderEntity order, InstrumentEntity instrument, OrderStatus status, String rejectionReason, String message) {
        var ticker = instrument != null ? instrument.getTicker() : UNKNOWN_INSTRUMENT;
        var name = instrument != null ? instrument.getName() : UNKNOWN_INSTRUMENT;

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getInstrumentId(),
                ticker,
                name,
                order.getSide(),
                order.getType(),
                order.getSize(),
                order.getPrice(),
                order.totalValue(),
                status != null ? status : order.getStatus(),
                order.getDatetime(),
                order.getIdempotencyKey(),
                rejectionReason,
                message
        );
    }
}
