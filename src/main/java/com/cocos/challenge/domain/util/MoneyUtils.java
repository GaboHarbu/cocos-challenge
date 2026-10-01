package com.cocos.challenge.domain.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Clean, standard financial math utility for Argentine Pesos (ARS).
 * Eliminates precision overkill: all currency amounts and percentages
 * use standard 2 decimal places with Banker's Rounding (HALF_EVEN).
 */
public final class MoneyUtils {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
    public static final BigDecimal ZERO_PERCENT = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

    private MoneyUtils() {
        // Utility class
    }

    /**
     * Scales and rounds a monetary amount in pesos to 2 decimal places.
     */
    public static BigDecimal roundMoney(BigDecimal value) {
        if (value == null) {
            return ZERO_MONEY;
        }
        return value.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Scales and rounds a percentage to 2 decimal places.
     */
    public static BigDecimal roundPercent(BigDecimal value) {
        if (value == null) {
            return ZERO_PERCENT;
        }
        return value.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Multiplies unit price (in pesos) by integer share quantity, rounded to 2 decimals.
     */
    public static BigDecimal multiply(BigDecimal price, int quantity) {
        if (price == null || quantity == 0) {
            return ZERO_MONEY;
        }
        return roundMoney(price.multiply(BigDecimal.valueOf(quantity)));
    }

    /**
     * Divides numerator by denominator with 2 decimal places and HALF_EVEN rounding.
     */
    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        return divide(numerator, denominator, SCALE);
    }

    /**
     * Divides numerator by denominator with specified scale and HALF_EVEN rounding.
     */
    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO_MONEY;
        }
        return numerator.divide(denominator, scale, ROUNDING_MODE);
    }

    /**
     * Standard financial percentage return formula: ((current - base) / base) * 100
     * Rounded to 2 decimal places with HALF_EVEN.
     */
    public static BigDecimal calculatePercentChange(BigDecimal current, BigDecimal base) {
        if (current == null || base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO_PERCENT;
        }
        return current.subtract(base)
                .multiply(BigDecimal.valueOf(100))
                .divide(base, SCALE, ROUNDING_MODE);
    }
}
