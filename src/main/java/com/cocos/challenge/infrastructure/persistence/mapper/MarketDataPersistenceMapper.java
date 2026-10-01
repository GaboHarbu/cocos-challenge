package com.cocos.challenge.infrastructure.persistence.mapper;

import com.cocos.challenge.domain.model.MarketData;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import org.springframework.stereotype.Component;

@Component
public class MarketDataPersistenceMapper {

    public MarketData toDomain(MarketDataEntity entity) {
        if (entity == null) {
            return null;
        }
        return MarketData.builder()
                .id(entity.getId())
                .instrumentId(entity.getInstrumentId())
                .high(entity.getHigh())
                .low(entity.getLow())
                .open(entity.getOpen())
                .close(entity.getClose())
                .previousClose(entity.getPreviousClose())
                .date(entity.getDate())
                .build();
    }

    public MarketDataEntity toEntity(MarketData domain) {
        if (domain == null) {
            return null;
        }
        return new MarketDataEntity(
                domain.id(),
                domain.instrumentId(),
                domain.high(),
                domain.low(),
                domain.open(),
                domain.close(),
                domain.previousClose(),
                domain.date()
        );
    }
}
