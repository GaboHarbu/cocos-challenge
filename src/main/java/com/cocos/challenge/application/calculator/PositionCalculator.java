package com.cocos.challenge.application.calculator;

import com.cocos.challenge.domain.model.Holding;
import com.cocos.challenge.domain.model.MarketData;
import com.cocos.challenge.domain.model.Position;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.mapper.MarketDataPersistenceMapper;
import com.cocos.challenge.domain.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class PositionCalculator {

    private static final String DEFAULT_UNKNOWN_INSTRUMENT = "N/A";

    private final MarketDataPersistenceMapper marketDataPersistenceMapper;

    public PositionCalculator() {
        this(new MarketDataPersistenceMapper());
    }

    public PositionCalculator(MarketDataPersistenceMapper marketDataPersistenceMapper) {
        this.marketDataPersistenceMapper = marketDataPersistenceMapper != null
                ? marketDataPersistenceMapper
                : new MarketDataPersistenceMapper();
    }

    public List<Position> calculatePositions(
            Map<Long, Holding> holdings,
            Map<Long, InstrumentEntity> instruments,
            Map<Long, MarketDataEntity> marketDataMap
    ) {
        if (hasNoHoldings(holdings)) {
            return List.of();
        }

        return holdings.entrySet().stream()
                .filter(entry -> entry.getValue().hasShares())
                .map(entry -> {
                    var instrumentId = entry.getKey();
                    var holding = entry.getValue();
                    var instrument = instruments != null ? instruments.get(instrumentId) : null;
                    var mdEntity = marketDataMap != null ? marketDataMap.get(instrumentId) : null;
                    var marketData = mdEntity != null ? marketDataPersistenceMapper.toDomain(mdEntity) : null;

                    var close = extractClosePrice(marketData);
                    var dailyReturn = extractDailyReturn(marketData);
                    var avgCost = holding.averageCost(close);

                    return new Position(
                            instrumentId,
                            instrument != null ? instrument.getTicker() : DEFAULT_UNKNOWN_INSTRUMENT,
                            instrument != null ? instrument.getName() : DEFAULT_UNKNOWN_INSTRUMENT,
                            holding.getCurrentShares(),
                            close,
                            dailyReturn,
                            avgCost
                    );
                })
                .toList();
    }

    private boolean hasNoHoldings(Map<Long, Holding> holdings) {
        return holdings == null || holdings.isEmpty();
    }

    private BigDecimal extractClosePrice(MarketData marketData) {
        if (marketData != null && marketData.close() != null) {
            return MoneyUtils.roundMoney(marketData.close());
        }
        return MoneyUtils.ZERO_MONEY;
    }

    private BigDecimal extractDailyReturn(MarketData marketData) {
        if (marketData != null) {
            return marketData.calculateDailyReturnPercent();
        }
        return MoneyUtils.ZERO_PERCENT;
    }
}
