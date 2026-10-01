package com.cocos.challenge.application.validator;

import com.cocos.challenge.application.calculator.CashCalculator;
import com.cocos.challenge.application.calculator.HoldingCalculator;
import com.cocos.challenge.application.dto.order.SendOrderRequest;
import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.exception.InvalidOrderException;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class OrderValidator {

    private static final Pattern VALID_IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    private final CashCalculator cashCalculator;
    private final HoldingCalculator holdingCalculator;

    public void validateNotCurrency(InstrumentEntity instrument) {
        if (isCurrencyInstrument(instrument)) {
            throw new InvalidOrderException("Market orders cannot be placed on currency instruments: " + instrument.getTicker());
        }
    }

    public void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return;
        }

        var trimmed = idempotencyKey.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidOrderException("Idempotency-Key header cannot be blank if provided.");
        }

        if (trimmed.length() > 64) {
            throw new InvalidOrderException("Idempotency-Key header cannot exceed 64 characters.");
        }

        if (!VALID_IDEMPOTENCY_KEY_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidOrderException("Idempotency-Key contains invalid characters. Must consist of alphanumeric characters, dashes, or underscores.");
        }
    }

    public void validateIdempotentMatches(OrderEntity existingOrder, SendOrderRequest request, InstrumentEntity instrument) {
        if (!existingOrder.getUserId().equals(request.userId())) {
            throw new InvalidOrderException("Idempotency key already belongs to a different user.");
        }
        if (existingOrder.getSide() != request.side() || existingOrder.getType() != request.type()) {
            throw new InvalidOrderException("Idempotency key reused with different order type or side.");
        }
        if (request.hasInstrumentId() && !existingOrder.getInstrumentId().equals(request.instrumentId())) {
            throw new InvalidOrderException("Idempotency key reused with different instrument parameters.");
        }
        if (request.hasTicker() && instrument != null && !instrument.getTicker().equalsIgnoreCase(request.ticker().trim())) {
            throw new InvalidOrderException("Idempotency key reused with different instrument ticker.");
        }
    }

    private boolean isCurrencyInstrument(InstrumentEntity instrument) {
        return instrument != null && instrument.isMoney();
    }

    public OrderValidationResult validateFundsAndShares(SendOrderRequest request, BigDecimal price, int size, List<OrderEntity> orders, Long instrumentId) {
        var status = request.type() == OrderType.MARKET ? OrderStatus.FILLED : OrderStatus.NEW;

        if (request.side() == OrderSide.BUY) {
            var availableFunds = cashCalculator.calculateAvailableFunds(orders);
            var totalCost = com.cocos.challenge.domain.util.MoneyUtils.multiply(price, size);
            if (hasInsufficientFunds(availableFunds, totalCost)) {
                return new OrderValidationResult(OrderStatus.REJECTED,
                        "Insufficient funds to buy. Available funds: $" + availableFunds + ", Required: $" + totalCost);
            }
        } else if (request.side() == OrderSide.SELL) {
            var availableShares = holdingCalculator.calculateAvailableShares(orders, instrumentId);
            if (hasInsufficientShares(availableShares, size)) {
                return new OrderValidationResult(OrderStatus.REJECTED,
                        "Insufficient shares to sell. Available shares: " + availableShares + ", Required: " + size);
            }
        } else {
            throw new InvalidOrderException("Unsupported order side: " + request.side());
        }

        return new OrderValidationResult(status, null);
    }

    private boolean hasInsufficientFunds(BigDecimal availableFunds, BigDecimal totalCost) {
        return availableFunds.compareTo(totalCost) < 0;
    }

    private boolean hasInsufficientShares(int availableShares, int requiredShares) {
        return availableShares < requiredShares;
    }

    public record OrderValidationResult(OrderStatus status, String rejectionReason) {}
}
