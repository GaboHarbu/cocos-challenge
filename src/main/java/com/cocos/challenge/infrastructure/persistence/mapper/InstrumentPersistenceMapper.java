package com.cocos.challenge.infrastructure.persistence.mapper;

import com.cocos.challenge.domain.model.Instrument;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import org.springframework.stereotype.Component;

@Component
public class InstrumentPersistenceMapper {

    public Instrument toDomain(InstrumentEntity entity) {
        if (entity == null) {
            return null;
        }
        return Instrument.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .name(entity.getName())
                .type(entity.getType())
                .build();
    }

    public InstrumentEntity toEntity(Instrument domain) {
        if (domain == null) {
            return null;
        }
        return new InstrumentEntity(domain.id(), domain.ticker(), domain.name(), domain.type());
    }
}
