package com.cocos.challenge.domain.model;

import com.cocos.challenge.domain.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.List;

public record Portfolio(
        Long userId,
        String accountNumber,
        BigDecimal totalAccountValue,
        BigDecimal availableCash,
        BigDecimal totalCash,
        List<Position> positions
) {
    public Portfolio(Long userId, String accountNumber, BigDecimal availableCash,
                     BigDecimal totalCash, List<Position> positions) {
        this(
                userId,
                accountNumber,
                calculateTotalAccountValue(totalCash, positions),
                MoneyUtils.roundMoney(availableCash),
                MoneyUtils.roundMoney(totalCash),
                safePositionsList(positions)
        );
    }

    public static Portfolio create(Long userId, String accountNumber, BigDecimal availableCash,
                                   BigDecimal totalCash, List<Position> positions) {
        return new Portfolio(userId, accountNumber, availableCash, totalCash, positions);
    }

    private static BigDecimal calculateTotalAccountValue(BigDecimal totalCash, List<Position> positions) {
        var baseCash = MoneyUtils.roundMoney(totalCash);
        if (hasNoPositions(positions)) {
            return baseCash;
        }
        var positionsMarketValue = BigDecimal.ZERO;
        for (var position : positions) {
            if (position != null && position.marketValue() != null) {
                positionsMarketValue = positionsMarketValue.add(position.marketValue());
            }
        }
        return MoneyUtils.roundMoney(baseCash.add(positionsMarketValue));
    }

    private static List<Position> safePositionsList(List<Position> positions) {
        if (positions == null || positions.isEmpty()) {
            return List.of();
        }
        return List.copyOf(positions);
    }

    private static boolean hasNoPositions(List<Position> positions) {
        return positions == null || positions.isEmpty();
    }
}
