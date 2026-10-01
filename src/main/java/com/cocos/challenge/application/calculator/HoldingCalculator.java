package com.cocos.challenge.application.calculator;

import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.model.Holding;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HoldingCalculator {

    /**
     * Aggregates share quantities and acquisition costs from filled orders, ignoring currency instruments.
     */
    public Map<Long, Holding> aggregateHoldings(List<OrderEntity> orders, Map<Long, InstrumentEntity> instruments) {
        var holdings = new HashMap<Long, Holding>();

        for (var order : orders) {
            var instrument = instruments != null ? instruments.get(order.getInstrumentId()) : null;
            if (isCurrencyInstrument(instrument)) {
                continue;
            }

            var side = order.getSide();
            if (side == null) {
                continue;
            }

            var size = order.getSize() != null ? order.getSize() : 0;
            if (size <= 0) {
                continue;
            }

            var holding = holdings.computeIfAbsent(order.getInstrumentId(), k -> new Holding());

            switch (side) {
                case BUY -> holding.buy(size, order.totalValue());
                case SELL -> {
                    int sellableShares = Math.min(size, holding.getCurrentShares());
                    if (sellableShares > 0) {
                        holding.sell(sellableShares);
                    }
                }
                default -> { /* Cash movements do not impact stock holdings */ }
            }
        }

        return holdings;
    }

    /**
     * Computes available (uncommitted) shares for a given instrument.
     */
    public int calculateAvailableShares(List<OrderEntity> orders, Long instrumentId) {
        int ownedShares = 0;
        int committedShares = 0;

        for (var o : orders) {
            if (matchesInstrument(o, instrumentId)) {
                int sz = o.getSize() != null ? o.getSize() : 0;
                var side = o.getSide();
                if (side == null) {
                    continue;
                }

                if (o.isFilled()) {
                    if (side == OrderSide.BUY) {
                        ownedShares += sz;
                    } else if (side == OrderSide.SELL) {
                        ownedShares = Math.max(0, ownedShares - sz);
                    }
                } else if (isCommittedSellOrder(o, side)) {
                    committedShares += sz;
                }
            }
        }
        return Math.max(0, ownedShares - committedShares);
    }

    private boolean isCurrencyInstrument(InstrumentEntity instrument) {
        return instrument != null && instrument.isMoney();
    }

    private boolean matchesInstrument(OrderEntity order, Long instrumentId) {
        return order.getInstrumentId() != null && order.getInstrumentId().equals(instrumentId);
    }

    private boolean isCommittedSellOrder(OrderEntity order, OrderSide side) {
        return order.isNew() && side == OrderSide.SELL;
    }
}
