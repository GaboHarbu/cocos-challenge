package com.cocos.challenge.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyUtilsTest {

    @Test
    @DisplayName("roundMoney scales to 2 decimals using HALF_EVEN")
    void testRoundMoney() {
        assertEquals(BigDecimal.valueOf(100.55), MoneyUtils.roundMoney(BigDecimal.valueOf(100.546)));
        // Banker's rounding tests: rounds to nearest even
        assertEquals(BigDecimal.valueOf(100.54), MoneyUtils.roundMoney(BigDecimal.valueOf(100.545)));
        assertEquals(BigDecimal.valueOf(100.56), MoneyUtils.roundMoney(BigDecimal.valueOf(100.555)));
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), MoneyUtils.roundMoney(null));
    }

    @Test
    @DisplayName("roundPercent scales to 2 decimals")
    void testRoundPercent() {
        assertEquals(BigDecimal.valueOf(12.35), MoneyUtils.roundPercent(BigDecimal.valueOf(12.3456)));
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), MoneyUtils.roundPercent(null));
    }

    @Test
    @DisplayName("multiply computes price * quantity rounded to money scale")
    void testMultiply() {
        assertEquals(BigDecimal.valueOf(1050.00).setScale(2), MoneyUtils.multiply(BigDecimal.valueOf(105.00), 10));
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), MoneyUtils.multiply(null, 10));
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), MoneyUtils.multiply(BigDecimal.valueOf(105.00), 0));
    }

    @Test
    @DisplayName("divide handles division scale and zero denominator safely")
    void testDivide() {
        var result = MoneyUtils.divide(BigDecimal.valueOf(100), BigDecimal.valueOf(3), 4);
        assertEquals(BigDecimal.valueOf(33.3333), result);

        assertEquals(BigDecimal.valueOf(0.00).setScale(2), MoneyUtils.divide(BigDecimal.valueOf(100), BigDecimal.ZERO, 2));
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), MoneyUtils.divide(null, BigDecimal.valueOf(5), 2));
    }
}
