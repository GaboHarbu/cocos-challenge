package com.cocos.challenge.application.dto.portfolio;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PortfolioResponse(
        Long userId,
        String accountNumber,
        BigDecimal totalAccountValue,
        BigDecimal availableCash,
        BigDecimal totalCash,
        List<PositionResponse> positions
) {}
