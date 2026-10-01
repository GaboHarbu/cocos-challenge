package com.cocos.challenge.domain.model;

import com.cocos.challenge.domain.util.MoneyUtils;

import java.math.BigDecimal;

/**
 * Represents the aggregated holdings and acquisition cost of an investment
 * instrument.
 *
 * Design Decision:
 * Calculates the weighted average acquisition price based on all historical buy
 * executions.
 * Sell executions reduce the current quantity held, but do not alter the
 * historical weighted
 * average acquisition price.
 */
public final class Holding {

    private int currentShares;
    private int totalBuyShares;
    private BigDecimal totalBuyCost = BigDecimal.ZERO;

    public Holding() {
    }

    public Holding(int currentShares, int totalBuyShares, BigDecimal totalBuyCost) {
        if (currentShares < 0) {
            throw new IllegalArgumentException("Current shares cannot be negative.");
        }
        if (totalBuyShares < 0) {
            throw new IllegalArgumentException("Total buy shares cannot be negative.");
        }
        this.currentShares = currentShares;
        this.totalBuyShares = totalBuyShares;
        this.totalBuyCost = defaultZero(totalBuyCost);
    }

    public void buy(int shares, BigDecimal cost) {
        if (!hasPositiveShares(shares)) {
            throw new IllegalArgumentException("Shares to buy must be greater than zero.");
        }
        this.currentShares += shares;
        this.totalBuyShares += shares;
        if (cost != null) {
            this.totalBuyCost = this.totalBuyCost.add(cost);
        }
    }

    public void sell(int shares) {
        if (!hasPositiveShares(shares)) {
            throw new IllegalArgumentException("Shares to sell must be greater than zero.");
        }
        if (shares > this.currentShares) {
            throw new IllegalArgumentException(
                    "Cannot sell more shares than currently held. Currently held: " + this.currentShares
                            + ", requested: " + shares);
        }
        this.currentShares -= shares;
    }

    public boolean hasShares() {
        return this.currentShares > 0;
    }

    public boolean hasBuyShares() {
        return this.totalBuyShares > 0;
    }

    public BigDecimal averageCost(BigDecimal fallback) {
        if (!hasBuyShares()) {
            return MoneyUtils.roundMoney(fallback);
        }
        return MoneyUtils.divide(totalBuyCost, BigDecimal.valueOf(totalBuyShares));
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static boolean hasPositiveShares(int shares) {
        return shares > 0;
    }

    public int getCurrentShares() {
        return currentShares;
    }

    public int getTotalBuyShares() {
        return totalBuyShares;
    }

    public BigDecimal getTotalBuyCost() {
        return totalBuyCost;
    }
}
