package com.cocos.challenge.domain.model;

import com.cocos.challenge.domain.util.MoneyUtils;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record MarketData(
        Long id,
        Long instrumentId,
        BigDecimal high,
        BigDecimal low,
        BigDecimal open,
        BigDecimal close,
        BigDecimal previousClose,
        LocalDate date) {
    public boolean hasValidPricesForReturn() {
        return close != null && previousClose != null && previousClose.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal calculateDailyReturnPercent() {
        return MoneyUtils.calculatePercentChange(close, previousClose);
    }
}
