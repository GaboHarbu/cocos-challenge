package com.cocos.challenge.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoldingTest {

    @Test
    @DisplayName("Holding tracks buys, sells and preserves historical weighted average acquisition price")
    void testHoldingWeightedAverageAcquisitionPrice() {
        Holding holding = new Holding();

        // 1. Buy 10 @ $100 = $1,000
        holding.buy(10, BigDecimal.valueOf(1000.00));
        assertTrue(holding.hasShares());
        assertEquals(10, holding.getCurrentShares());
        assertEquals(10, holding.getTotalBuyShares());
        assertEquals(BigDecimal.valueOf(1000.00), holding.getTotalBuyCost());
        assertEquals(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_EVEN), holding.averageCost(BigDecimal.ZERO));

        // 2. Buy 10 @ $200 = $2,000 (total: 20 shares, $3,000 cost -> avg $150)
        holding.buy(10, BigDecimal.valueOf(2000.00));
        assertEquals(20, holding.getCurrentShares());
        assertEquals(20, holding.getTotalBuyShares());
        assertEquals(BigDecimal.valueOf(3000.00), holding.getTotalBuyCost());
        assertEquals(BigDecimal.valueOf(150.00).setScale(2, RoundingMode.HALF_EVEN), holding.averageCost(BigDecimal.ZERO));

        // 3. Sell 10 shares -> currentShares becomes 10, but historical avg cost remains $150
        holding.sell(10);
        assertEquals(10, holding.getCurrentShares());
        assertEquals(20, holding.getTotalBuyShares());
        assertEquals(BigDecimal.valueOf(150.00).setScale(2, RoundingMode.HALF_EVEN), holding.averageCost(BigDecimal.ZERO));

        // 4. Sell remaining 10 shares -> currentShares becomes 0
        holding.sell(10);
        assertEquals(0, holding.getCurrentShares());
        assertFalse(holding.hasShares());
    }

    @Test
    @DisplayName("Holding returns fallback when no buy executions exist")
    void testHoldingFallbackWhenNoBuys() {
        Holding holding = new Holding();
        assertFalse(holding.hasShares());
        assertEquals(BigDecimal.valueOf(45.00).setScale(2, RoundingMode.HALF_EVEN),
                holding.averageCost(BigDecimal.valueOf(45.00)));
    }

    @Test
    @DisplayName("Holding cannot sell more shares than currently held")
    void testCannotSellMoreThanHeld() {
        Holding holding = new Holding();
        holding.buy(10, BigDecimal.valueOf(100.00));

        assertThrows(IllegalArgumentException.class, () -> holding.sell(15));
        assertEquals(10, holding.getCurrentShares());
    }

    @Test
    @DisplayName("Holding enforces positive share counts and non-negative constructor invariants")
    void testHoldingInvariants() {
        Holding holding = new Holding();
        assertThrows(IllegalArgumentException.class, () -> holding.buy(0, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> holding.buy(-5, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> holding.sell(0));
        assertThrows(IllegalArgumentException.class, () -> holding.sell(-2));

        assertThrows(IllegalArgumentException.class, () -> new Holding(-1, 0, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Holding(0, -1, BigDecimal.ZERO));
    }
}
