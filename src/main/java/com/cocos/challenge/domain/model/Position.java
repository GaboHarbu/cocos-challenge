package com.cocos.challenge.domain.model;

import com.cocos.challenge.domain.util.MoneyUtils;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record Position(
        Long instrumentId,
        String ticker,
        String name,
        Integer quantity,
        BigDecimal marketPrice,
        BigDecimal marketValue,
        BigDecimal dailyReturnPercent,
        BigDecimal totalReturnPercent
) {
    public Position(Long instrumentId, String ticker, String name, Integer quantity,
                    BigDecimal marketPrice, BigDecimal dailyReturnPercent, BigDecimal averageCostPrice) {
        this(
                instrumentId,
                ticker,
                name,
                quantity,
                MoneyUtils.roundMoney(marketPrice),
                MoneyUtils.multiply(marketPrice, quantity != null ? quantity : 0),
                MoneyUtils.roundPercent(dailyReturnPercent),
                MoneyUtils.calculatePercentChange(marketPrice, averageCostPrice)
        );
    }
}
