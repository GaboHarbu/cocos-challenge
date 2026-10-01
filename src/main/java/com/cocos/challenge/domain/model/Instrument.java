package com.cocos.challenge.domain.model;

import com.cocos.challenge.domain.enums.InstrumentType;
import lombok.Builder;

@Builder
public record Instrument(
        Long id,
        String ticker,
        String name,
        InstrumentType type
) {
    public static final String CURRENCY_TICKER_ARS = "ARS";

    public boolean isMoney() {
        return isCurrencyType() || isCurrencyTicker();
    }

    public boolean isCurrencyType() {
        return type == InstrumentType.MONEDA;
    }

    public boolean isCurrencyTicker() {
        return CURRENCY_TICKER_ARS.equalsIgnoreCase(ticker);
    }
}
