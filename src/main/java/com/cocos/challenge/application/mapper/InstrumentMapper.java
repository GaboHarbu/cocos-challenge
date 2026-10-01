package com.cocos.challenge.application.mapper;

import com.cocos.challenge.application.dto.instrument.InstrumentDto;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.mapper.MarketDataPersistenceMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InstrumentMapper {

    private final MarketDataPersistenceMapper marketDataPersistenceMapper;

    public InstrumentMapper() {
        this(new MarketDataPersistenceMapper());
    }

    public InstrumentMapper(MarketDataPersistenceMapper marketDataPersistenceMapper) {
        this.marketDataPersistenceMapper = marketDataPersistenceMapper != null
                ? marketDataPersistenceMapper
                : new MarketDataPersistenceMapper();
    }

    public InstrumentDto toDto(InstrumentEntity entity, MarketDataEntity md) {
        var latestPrice = md != null ? md.getClose() : null;
        var dailyReturn = resolveDailyReturn(md);

        return new InstrumentDto(
                entity.getId(),
                entity.getTicker(),
                entity.getName(),
                entity.getType(),
                latestPrice,
                dailyReturn
        );
    }

    private BigDecimal resolveDailyReturn(MarketDataEntity md) {
        if (md == null) {
            return null;
        }
        var domain = marketDataPersistenceMapper.toDomain(md);
        return domain.hasValidPricesForReturn() ? domain.calculateDailyReturnPercent() : null;
    }
}
