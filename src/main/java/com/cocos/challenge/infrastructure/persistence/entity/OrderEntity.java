package com.cocos.challenge.infrastructure.persistence.entity;

import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrumentid", nullable = false)
    private Long instrumentId;

    @Column(name = "userid", nullable = false)
    private Long userId;

    @Column(name = "size", nullable = false)
    private Integer size;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "datetime", nullable = false)
    private LocalDateTime datetime;

    @Column(name = "idempotency_key", length = 255, unique = true)
    private String idempotencyKey;

    public OrderEntity(Long id, Long instrumentId, Long userId, Integer size, BigDecimal price,
                       String type, String side, String status, LocalDateTime datetime, String idempotencyKey) {
        this(
                id,
                instrumentId,
                userId,
                size,
                price,
                type != null ? OrderType.valueOf(type.toUpperCase()) : null,
                side != null ? OrderSide.valueOf(side.toUpperCase()) : null,
                status != null ? OrderStatus.valueOf(status.toUpperCase()) : null,
                datetime,
                idempotencyKey
        );
    }

    public boolean isFilled() {
        return status == OrderStatus.FILLED;
    }

    public boolean isNew() {
        return status == OrderStatus.NEW;
    }

    public BigDecimal totalValue() {
        return com.cocos.challenge.domain.util.MoneyUtils.multiply(resolveEffectivePrice(), resolveEffectiveSize());
    }

    private BigDecimal resolveEffectivePrice() {
        return price != null ? price : BigDecimal.ONE;
    }

    private int resolveEffectiveSize() {
        return size != null ? size : 0;
    }
}
