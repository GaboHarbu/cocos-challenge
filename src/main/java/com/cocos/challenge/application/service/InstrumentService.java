package com.cocos.challenge.application.service;

import com.cocos.challenge.application.dto.instrument.InstrumentDto;
import com.cocos.challenge.application.mapper.InstrumentMapper;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final MarketDataRepository marketDataRepository;
    private final InstrumentMapper instrumentMapper;

    @Transactional(readOnly = true)
    public Page<InstrumentDto> search(String query, Pageable pageable) {
        var cleanQuery = query != null ? query.trim() : "";
        var page = instrumentRepository.findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(cleanQuery, cleanQuery, pageable);
        var marketDataMap = getLatestMarketDataMap(page.getContent());

        log.info("[MARKET] [INSTRUMENT_SEARCH] query=\"{}\" totalResults={}",
                cleanQuery, page.getTotalElements());

        return page.map(entity -> instrumentMapper.toDto(entity, marketDataMap.get(entity.getId())));
    }

    private Map<Long, MarketDataEntity> getLatestMarketDataMap(List<InstrumentEntity> instruments) {
        if (instruments == null || instruments.isEmpty()) {
            return Map.of();
        }

        var instrumentIds = instruments.stream()
                .filter(Objects::nonNull)
                .map(InstrumentEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (instrumentIds.isEmpty()) {
            return Map.of();
        }

        var marketDataEntities = marketDataRepository.findByInstrumentIdInOrderByDateDescIdDesc(instrumentIds);
        var marketDataMap = new HashMap<Long, MarketDataEntity>();
        for (var md : marketDataEntities) {
            if (md != null && md.getInstrumentId() != null) {
                marketDataMap.putIfAbsent(md.getInstrumentId(), md);
            }
        }
        return marketDataMap;
    }
}
