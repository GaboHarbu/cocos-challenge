package com.cocos.challenge.application.dto.order;

import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.exception.InvalidOrderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SendOrderRequestTest {

    @Test
    @DisplayName("Compact Constructor throws when both instrumentId and ticker are missing")
    void testMissingInstrumentIdAndTicker() {
        assertThrows(InvalidOrderException.class, () ->
                SendOrderRequest.builder()
                        .userId(1L)
                        .side(OrderSide.BUY)
                        .type(OrderType.MARKET)
                        .size(10)
                        .build()
        );
    }

    @Test
    @DisplayName("Compact Constructor throws when LIMIT order does not specify price")
    void testLimitOrderMissingPrice() {
        assertThrows(InvalidOrderException.class, () ->
                SendOrderRequest.builder()
                        .userId(1L)
                        .ticker("GGAL")
                        .side(OrderSide.BUY)
                        .type(OrderType.LIMIT)
                        .size(10)
                        .price(null)
                        .build()
        );
    }

    @Test
    @DisplayName("Compact Constructor throws when neither size nor amount are provided")
    void testNeitherSizeNorAmountProvided() {
        assertThrows(InvalidOrderException.class, () ->
                SendOrderRequest.builder()
                        .userId(1L)
                        .ticker("GGAL")
                        .side(OrderSide.BUY)
                        .type(OrderType.MARKET)
                        .build()
        );
    }

    @Test
    @DisplayName("Compact Constructor throws when both size and amount are provided simultaneously")
    void testBothSizeAndAmountProvided() {
        assertThrows(InvalidOrderException.class, () ->
                SendOrderRequest.builder()
                        .userId(1L)
                        .ticker("GGAL")
                        .side(OrderSide.BUY)
                        .type(OrderType.MARKET)
                        .size(10)
                        .amount(BigDecimal.valueOf(1000))
                        .build()
        );
    }

    @Test
    @DisplayName("Compact Constructor succeeds when inputs are valid")
    void testValidRequest() {
        assertDoesNotThrow(() ->
                SendOrderRequest.builder()
                        .userId(1L)
                        .ticker("GGAL")
                        .side(OrderSide.BUY)
                        .type(OrderType.MARKET)
                        .size(10)
                        .build()
        );
    }
}
