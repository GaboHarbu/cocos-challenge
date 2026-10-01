package com.cocos.challenge.infrastructure.persistence.mapper;

import com.cocos.challenge.domain.model.Order;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {

    public Order toDomain(OrderEntity entity) {
        if (entity == null) {
            return null;
        }
        return Order.builder()
                .id(entity.getId())
                .instrumentId(entity.getInstrumentId())
                .userId(entity.getUserId())
                .size(entity.getSize())
                .price(entity.getPrice())
                .type(entity.getType())
                .side(entity.getSide())
                .status(entity.getStatus())
                .datetime(entity.getDatetime())
                .idempotencyKey(entity.getIdempotencyKey())
                .build();
    }

    public OrderEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }
        return OrderEntity.builder()
                .id(domain.getId())
                .instrumentId(domain.getInstrumentId())
                .userId(domain.getUserId())
                .size(domain.getSize())
                .price(domain.getPrice())
                .type(domain.getType())
                .side(domain.getSide())
                .status(domain.getStatus())
                .datetime(domain.getDatetime())
                .idempotencyKey(domain.getIdempotencyKey())
                .build();
    }
}
