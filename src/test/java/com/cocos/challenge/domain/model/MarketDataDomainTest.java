package com.cocos.challenge.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketDataDomainTest {

    @Test
    @DisplayName("Daily return percentage is calculated with half-even banker's rounding")
    void testCalculateDailyReturn() {
        // close = 105.00, previousClose = 100.00 -> +5.00%
        MarketData md = new MarketData(1L, 10L, BigDecimal.valueOf(110), BigDecimal.valueOf(95),
                BigDecimal.valueOf(99), BigDecimal.valueOf(105), BigDecimal.valueOf(100), LocalDate.now());

        assertEquals(BigDecimal.valueOf(5.00).setScale(2), md.calculateDailyReturnPercent());

        // close = 97.50, previousClose = 100.00 -> -2.50%
        MarketData mdDown = new MarketData(2L, 10L, BigDecimal.valueOf(102), BigDecimal.valueOf(96),
                BigDecimal.valueOf(100), BigDecimal.valueOf(97.50), BigDecimal.valueOf(100), LocalDate.now());

        assertEquals(BigDecimal.valueOf(-2.50).setScale(2), mdDown.calculateDailyReturnPercent());
    }

    @Test
    @DisplayName("Position calculates market value and returns properly")
    void testPositionCalculations() {
        // 10 shares at 120.00 market price, avg purchase cost 100.00 -> +20.00% total return
        Position pos = new Position(1L, "GGAL", "Grupo Financiero Galicia", 10,
                BigDecimal.valueOf(120.00), BigDecimal.valueOf(2.50), BigDecimal.valueOf(100.00));

        assertEquals(10, pos.quantity());
        assertEquals(BigDecimal.valueOf(120.00).setScale(2), pos.marketPrice());
        assertEquals(BigDecimal.valueOf(1200.00).setScale(2), pos.marketValue());
        assertEquals(BigDecimal.valueOf(2.50).setScale(2), pos.dailyReturnPercent());
        assertEquals(BigDecimal.valueOf(20.00).setScale(2), pos.totalReturnPercent());
    }

    @Test
    @DisplayName("Position returns 0.00% total return when average cost price is unavailable, never leaking daily return")
    void testPositionTotalReturnWhenCostUnavailable() {
        Position pos = new Position(1L, "GGAL", "Grupo Financiero Galicia", 10,
                BigDecimal.valueOf(120.00), BigDecimal.valueOf(5.50), BigDecimal.ZERO);

        assertEquals(BigDecimal.valueOf(5.50).setScale(2), pos.dailyReturnPercent());
        assertEquals(BigDecimal.ZERO.setScale(2), pos.totalReturnPercent());
    }
}
