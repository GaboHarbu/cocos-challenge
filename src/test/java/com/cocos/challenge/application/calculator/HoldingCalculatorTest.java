package com.cocos.challenge.application.calculator;

import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoldingCalculatorTest {

    private final HoldingCalculator calculator = new HoldingCalculator();

    @Test
    @DisplayName("Aggregates stock orders into Holdings while ignoring currency instruments")
    void testAggregateHoldings() {
        var ars = new InstrumentEntity(1L, "ARS", "Pesos", "MONEDA");
        var pamp = new InstrumentEntity(2L, "PAMP", "Pampa Energia", "ACCIONES");
        var instruments = Map.of(1L, ars, 2L, pamp);

        var orders = List.of(
                // Cash deposit (should be ignored for stock holdings)
                new OrderEntity(1L, 1L, 10L, 50000, BigDecimal.ONE, "MARKET", "CASH_IN", "FILLED", LocalDateTime.now(), null),
                // Buy 100 PAMP @ $50
                new OrderEntity(2L, 2L, 10L, 100, BigDecimal.valueOf(50.00), "MARKET", "BUY", "FILLED", LocalDateTime.now(), null),
                // Sell 30 PAMP @ $60
                new OrderEntity(3L, 2L, 10L, 30, BigDecimal.valueOf(60.00), "MARKET", "SELL", "FILLED", LocalDateTime.now(), null)
        );

        var holdings = calculator.aggregateHoldings(orders, instruments);
        assertFalse(holdings.containsKey(1L));
        assertTrue(holdings.containsKey(2L));

        var pampHolding = holdings.get(2L);
        assertEquals(70, pampHolding.getCurrentShares());
        assertEquals(100, pampHolding.getTotalBuyShares());
    }

    @Test
    @DisplayName("Calculates available shares deducting committed pending SELL limit orders")
    void testCalculateAvailableSharesWithCommittedSells() {
        var orders = List.of(
                // Owned: 100 shares filled
                new OrderEntity(1L, 2L, 10L, 100, BigDecimal.valueOf(50.00), "MARKET", "BUY", "FILLED", LocalDateTime.now(), null),
                // Committed pending SELL limit: 40 shares
                new OrderEntity(2L, 2L, 10L, 40, BigDecimal.valueOf(65.00), "LIMIT", "SELL", "NEW", LocalDateTime.now(), null)
        );

        // Available to sell: 100 - 40 = 60
        int availableShares = calculator.calculateAvailableShares(orders, 2L);
        assertEquals(60, availableShares);
    }
}
