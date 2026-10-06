package com.cocos.challenge.application.service;

import com.cocos.challenge.application.dto.portfolio.PositionResponse;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.entity.UserEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.repository.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private MarketDataRepository marketDataRepository;

    private PortfolioService portfolioService;

    private UserEntity sampleUser;
    private InstrumentEntity moneyInstrument;
    private InstrumentEntity pampaInstrument;

    @BeforeEach
    void setUp() {
        sampleUser = new UserEntity(1L, "emiliano@test.com", "10001");
        moneyInstrument = new InstrumentEntity(66L, "ARS", "PESOS", "MONEDA");
        pampaInstrument = new InstrumentEntity(106L, "PAMP", "Pampa Holding S.A.", "ACCIONES");

        portfolioService = new PortfolioService(
                userRepository,
                orderRepository,
                instrumentRepository,
                marketDataRepository,
                new com.cocos.challenge.application.calculator.CashCalculator(),
                new com.cocos.challenge.application.calculator.HoldingCalculator(),
                new com.cocos.challenge.application.calculator.PositionCalculator(),
                new com.cocos.challenge.application.mapper.PortfolioMapper()
        );
    }

    @Test
    @DisplayName("Calculates available pesos, holdings and total return using HALF_EVEN")
    void testGetPortfolioCalculation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        List<OrderEntity> orders = new ArrayList<>();
        // 1. CASH_IN 1,000,000 ARS (FILLED)
        orders.add(new OrderEntity(1L, 66L, 1L, 1000000, BigDecimal.ONE, "MARKET", "CASH_IN", "FILLED", LocalDateTime.now(), null));
        // 2. Buy 100 PAMP at $500 = $50,000 (FILLED)
        orders.add(new OrderEntity(2L, 106L, 1L, 100, BigDecimal.valueOf(500.00), "MARKET", "BUY", "FILLED", LocalDateTime.now(), null));
        // 3. CASH_OUT 10,000 ARS (FILLED)
        orders.add(new OrderEntity(3L, 66L, 1L, 10000, BigDecimal.ONE, "MARKET", "CASH_OUT", "FILLED", LocalDateTime.now(), null));
        // 4. Pending LIMIT BUY (NEW): 50 shares @ $480 = $24,000 committed reserves
        orders.add(new OrderEntity(4L, 106L, 1L, 50, BigDecimal.valueOf(480.00), "LIMIT", "BUY", "NEW", LocalDateTime.now(), null));

        when(orderRepository.findByUserIdOrderByDatetimeAsc(1L)).thenReturn(orders);
        when(instrumentRepository.findAllById(anyCollection())).thenReturn(List.of(moneyInstrument, pampaInstrument));

        // Market data: close = 550.00, previousClose = 500.00 (+10.00% daily return)
        var pampaMd = new MarketDataEntity(1L, 106L, BigDecimal.valueOf(560), BigDecimal.valueOf(490),
                BigDecimal.valueOf(510), BigDecimal.valueOf(550.00), BigDecimal.valueOf(500.00), LocalDate.now());
        when(marketDataRepository.findByInstrumentIdInOrderByDateDescIdDesc(Set.of(106L))).thenReturn(List.of(pampaMd));

        var response = portfolioService.getPortfolio(1L);

        assertNotNull(response);
        assertEquals(1L, response.userId());
        assertEquals("10001", response.accountNumber());

        // Settled Total Cash: 1,000,000 (CASH_IN) - 50,000 (BUY) - 10,000 (CASH_OUT) = 940,000.00
        assertEquals(BigDecimal.valueOf(940000.00).setScale(2), response.totalCash());

        // Available Cash (purchasing power): 940,000.00 - 24,000.00 (committed BUY order) = 916,000.00
        assertEquals(BigDecimal.valueOf(916000.00).setScale(2), response.availableCash());

        // Positions: 100 PAMP (FILLED only)
        assertFalse(response.positions().isEmpty());
        assertEquals(1, response.positions().size());
        PositionResponse position = response.positions().get(0);
        assertEquals("PAMP", position.ticker());
        assertEquals(100, position.size());
        assertEquals(BigDecimal.valueOf(550.00).setScale(2), position.marketPrice());
        assertEquals(BigDecimal.valueOf(55000.00).setScale(2), position.marketValue());
        // Return: ((550 - 500) / 500) * 100 = 10.00%
        assertEquals(BigDecimal.valueOf(10.00).setScale(2), position.dailyReturnPercent());
        assertEquals(BigDecimal.valueOf(10.00).setScale(2), position.totalReturnPercent());

        // Total account value: 940,000 (settled total cash) + 55,000 (positions) = 995,000.00
        assertEquals(BigDecimal.valueOf(995000.00).setScale(2), response.totalAccountValue());
    }
}
