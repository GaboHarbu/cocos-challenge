package com.cocos.challenge.application.dto.instrument;

import com.cocos.challenge.domain.enums.InstrumentType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record InstrumentDto(
        Long id,
        String ticker,
        String name,
        InstrumentType type,
        BigDecimal latestPrice,
        BigDecimal dailyReturnPercent
) {}
