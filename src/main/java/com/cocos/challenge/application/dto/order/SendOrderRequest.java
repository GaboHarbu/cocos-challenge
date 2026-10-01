package com.cocos.challenge.application.dto.order;

import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.exception.InvalidOrderException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record SendOrderRequest(
        @NotNull(message = "userId is required")
        Long userId,

        Long instrumentId,

        String ticker,

        @NotNull(message = "side is required (BUY or SELL)")
        OrderSide side,

        @NotNull(message = "type is required (MARKET or LIMIT)")
        OrderType type,

        @Positive(message = "size must be positive if provided")
        Integer size,

        @Positive(message = "amount must be positive if provided")
        BigDecimal amount,

        @Positive(message = "price must be positive if provided")
        BigDecimal price
) {
    public SendOrderRequest {
        if (!hasInstrumentIdentifier(instrumentId, ticker)) {
            throw new InvalidOrderException("Either instrumentId or ticker must be specified.");
        }
        if (isInvalidLimitOrder(type, price)) {
            throw new InvalidOrderException("Price is mandatory and must be greater than zero for LIMIT orders.");
        }
        boolean hasSize = hasValidSize(size);
        boolean hasAmount = hasValidAmount(amount);
        if (!hasSize && !hasAmount) {
            throw new InvalidOrderException("Either size (number of shares) or amount (cash to invest) must be specified.");
        }
        if (hasSize && hasAmount) {
            throw new InvalidOrderException("Cannot specify both 'size' and 'amount' simultaneously.");
        }
    }

    private static boolean hasInstrumentIdentifier(Long instrumentId, String ticker) {
        return instrumentId != null || (ticker != null && !ticker.isBlank());
    }

    private static boolean isInvalidLimitOrder(OrderType type, BigDecimal price) {
        return type == OrderType.LIMIT && (price == null || price.compareTo(BigDecimal.ZERO) <= 0);
    }

    private static boolean hasValidSize(Integer size) {
        return size != null && size > 0;
    }

    private static boolean hasValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasSize() {
        return hasValidSize(size);
    }

    public boolean hasAmount() {
        return hasValidAmount(amount);
    }

    public boolean hasInstrumentId() {
        return instrumentId != null;
    }

    public boolean hasTicker() {
        return ticker != null && !ticker.isBlank();
    }
}
