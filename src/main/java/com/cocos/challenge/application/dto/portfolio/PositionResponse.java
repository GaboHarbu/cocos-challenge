package com.cocos.challenge.application.dto.portfolio;

import com.cocos.challenge.domain.model.Position;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PositionResponse(
        Long instrumentId,
        String ticker,
        String name,
        Integer size,
        BigDecimal marketPrice,
        BigDecimal marketValue,
        BigDecimal dailyReturnPercent,
        BigDecimal totalReturnPercent
) {
    public static PositionResponse from(Position p) {
        return new PositionResponse(
                p.instrumentId(),
                p.ticker(),
                p.name(),
                p.quantity(),
                p.marketPrice(),
                p.marketValue(),
                p.dailyReturnPercent(),
                p.totalReturnPercent()
        );
    }
}

