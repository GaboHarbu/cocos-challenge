package com.cocos.challenge.application.resolver;

import com.cocos.challenge.application.dto.order.SendOrderRequest;
import com.cocos.challenge.domain.exception.InvalidOrderException;
import com.cocos.challenge.domain.util.MoneyUtils;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class OrderPriceResolver {

    private final MarketDataRepository marketDataRepository;

    public BigDecimal resolvePrice(SendOrderRequest request, InstrumentEntity instrument) {
        return switch (request.type()) {
            case MARKET -> resolveMarketPrice(instrument);
            case LIMIT -> resolveLimitPrice(request);
        };
    }

    private BigDecimal resolveMarketPrice(InstrumentEntity instrument) {
        var md = marketDataRepository.findFirstByInstrumentIdOrderByDateDescIdDesc(instrument.getId())
                .orElseThrow(() -> new InvalidOrderException("No market data available for instrument: " + instrument.getTicker()));

        if (!isPositive(md.getClose())) {
            throw new InvalidOrderException("Invalid close price for instrument: " + instrument.getTicker());
        }
        return MoneyUtils.roundMoney(md.getClose());
    }

    private BigDecimal resolveLimitPrice(SendOrderRequest request) {
        if (!isPositive(request.price())) {
            throw new InvalidOrderException("Price is mandatory and must be positive for LIMIT orders.");
        }
        return MoneyUtils.roundMoney(request.price());
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
