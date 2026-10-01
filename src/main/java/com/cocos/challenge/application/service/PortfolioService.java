package com.cocos.challenge.application.service;

import com.cocos.challenge.application.calculator.CashCalculator;
import com.cocos.challenge.application.calculator.HoldingCalculator;
import com.cocos.challenge.application.calculator.PositionCalculator;
import com.cocos.challenge.application.dto.portfolio.PortfolioResponse;
import com.cocos.challenge.application.mapper.PortfolioMapper;
import com.cocos.challenge.domain.exception.UserNotFoundException;
import com.cocos.challenge.domain.model.Portfolio;
import com.cocos.challenge.domain.model.Position;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.repository.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final MarketDataRepository marketDataRepository;
    private final CashCalculator cashCalculator;
    private final HoldingCalculator holdingCalculator;
    private final PositionCalculator positionCalculator;
    private final PortfolioMapper portfolioMapper;

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long userId) {
        log.info("[PORTFOLIO] [QUERY] Fetching portfolio valuation for userId={}", userId);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        var filledOrders = findFilledOrders(userId);
        var instruments = getInstrumentsMap(filledOrders);
        var availableCash = calculateAvailableCash(filledOrders, instruments);
        var positions = calculatePositions(filledOrders, instruments);

        var portfolio = new Portfolio(
                user.getId(),
                user.getAccountNumber(),
                availableCash,
                availableCash,
                positions);

        log.info(
                "[PORTFOLIO] [VALUATION] userId={} accountNumber={} totalAccountValue={} ARS availableCash={} ARS positionsCount={}",
                user.getId(), user.getAccountNumber(), portfolio.totalAccountValue(), portfolio.availableCash(),
                positions.size());

        return portfolioMapper.toResponse(portfolio);
    }

    private List<OrderEntity> findFilledOrders(Long userId) {
        return orderRepository.findByUserIdOrderByDatetimeAsc(userId).stream()
                .filter(OrderEntity::isFilled)
                .toList();
    }

    private Map<Long, InstrumentEntity> getInstrumentsMap(List<OrderEntity> orders) {
        var instrumentIds = orders.stream()
                .map(OrderEntity::getInstrumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (instrumentIds.isEmpty()) {
            return Map.of();
        }

        return instrumentRepository.findAllById(instrumentIds).stream()
                .collect(Collectors.toMap(InstrumentEntity::getId, i -> i));
    }

    private BigDecimal calculateAvailableCash(List<OrderEntity> orders, Map<Long, InstrumentEntity> instruments) {
        return cashCalculator.calculateAvailableCash(orders, instruments);
    }

    private List<Position> calculatePositions(List<OrderEntity> orders, Map<Long, InstrumentEntity> instruments) {
        var holdings = holdingCalculator.aggregateHoldings(orders, instruments);
        if (holdings.isEmpty()) {
            return List.of();
        }

        var marketDataMap = getLatestMarketData(holdings.keySet());
        return positionCalculator.calculatePositions(holdings, instruments, marketDataMap);
    }

    private Map<Long, MarketDataEntity> getLatestMarketData(Set<Long> instrumentIds) {
        if (instrumentIds == null || instrumentIds.isEmpty()) {
            return Map.of();
        }
        var marketDataEntities = marketDataRepository.findByInstrumentIdInOrderByDateDescIdDesc(instrumentIds);
        var marketDataMap = new HashMap<Long, MarketDataEntity>();
        for (var md : marketDataEntities) {
            marketDataMap.putIfAbsent(md.getInstrumentId(), md);
        }
        return marketDataMap;
    }
}
