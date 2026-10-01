package com.cocos.challenge.application.calculator;

import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.util.MoneyUtils;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class CashCalculator {

    /**
     * Calculates the net settled available cash from filled orders and movements.
     */
    public BigDecimal calculateAvailableCash(List<OrderEntity> orders, Map<Long, InstrumentEntity> instruments) {
        BigDecimal cash = BigDecimal.ZERO;

        for (var order : orders) {
            var instrument = instruments != null ? instruments.get(order.getInstrumentId()) : null;
            boolean isMoney = isCurrencyInstrument(instrument);
            var totalValue = order.totalValue();
            var side = order.getSide();

            if (side == null) {
                continue;
            }

            cash = switch (side) {
                case CASH_IN -> cash.add(totalValue);
                case CASH_OUT -> cash.subtract(totalValue);
                case BUY -> isMoney ? cash : cash.subtract(totalValue);
                case SELL -> isMoney ? cash : cash.add(totalValue);
            };
        }

        return MoneyUtils.roundMoney(cash);
    }

    /**
     * Calculates purchasing power (available funds minus pending/committed BUY orders).
     */
    public BigDecimal calculateAvailableFunds(List<OrderEntity> orders) {
        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal committedCash = BigDecimal.ZERO;

        for (var o : orders) {
            var totalVal = o.totalValue();
            var side = o.getSide();
            if (side == null) {
                continue;
            }

            if (o.isFilled()) {
                totalCash = switch (side) {
                    case CASH_IN, SELL -> totalCash.add(totalVal);
                    case CASH_OUT, BUY -> totalCash.subtract(totalVal);
                };
            } else if (isCommittedBuyOrder(o, side)) {
                committedCash = committedCash.add(totalVal);
            }
        }

        return MoneyUtils.roundMoney(totalCash.subtract(committedCash));
    }

    private boolean isCurrencyInstrument(InstrumentEntity instrument) {
        return instrument != null && instrument.isMoney();
    }

    private boolean isCommittedBuyOrder(OrderEntity order, OrderSide side) {
        return order.isNew() && side == OrderSide.BUY;
    }
}
