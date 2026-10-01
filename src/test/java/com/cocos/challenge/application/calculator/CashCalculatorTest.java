package com.cocos.challenge.application.calculator;

import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CashCalculatorTest {

    private final CashCalculator calculator = new CashCalculator();

    @Test
    @DisplayName("Calculates settled available cash from deposits, withdrawals and filled trades")
    void testCalculateAvailableCash() {
        var ars = new InstrumentEntity(1L, "ARS", "Pesos", "MONEDA");
        var ggal = new InstrumentEntity(2L, "GGAL", "Grupo Galicia", "ACCIONES");
        var instruments = Map.of(1L, ars, 2L, ggal);

        var orders = List.of(
                // Deposit 50,000 ARS
                new OrderEntity(1L, 1L, 10L, 50000, BigDecimal.ONE, "MARKET", "CASH_IN", "FILLED", LocalDateTime.now(), null),
                // Buy 100 GGAL @ $200 = -$20,000
                new OrderEntity(2L, 2L, 10L, 100, BigDecimal.valueOf(200.00), "MARKET", "BUY", "FILLED", LocalDateTime.now(), null),
                // Sell 20 GGAL @ $250 = +$5,000
                new OrderEntity(3L, 2L, 10L, 20, BigDecimal.valueOf(250.00), "MARKET", "SELL", "FILLED", LocalDateTime.now(), null),
                // Withdrawal 10,000 ARS = -$10,000
                new OrderEntity(4L, 1L, 10L, 10000, BigDecimal.ONE, "MARKET", "CASH_OUT", "FILLED", LocalDateTime.now(), null)
        );

        // Expected: 50,000 - 20,000 + 5,000 - 10,000 = 25,000.00
        var availableCash = calculator.calculateAvailableCash(orders, instruments);
        assertEquals(BigDecimal.valueOf(25000.00).setScale(2, RoundingMode.HALF_EVEN), availableCash);
    }

    @Test
    @DisplayName("Calculates purchasing power deducting committed pending BUY limit orders")
    void testCalculateAvailableFundsWithCommittedOrders() {
        var orders = List.of(
                // Settled Cash: +10,000
                new OrderEntity(1L, 1L, 10L, 10000, BigDecimal.ONE, "MARKET", "CASH_IN", "FILLED", LocalDateTime.now(), null),
                // Pending LIMIT BUY: 10 shares @ $300 = $3,000 committed
                new OrderEntity(2L, 2L, 10L, 10, BigDecimal.valueOf(300.00), "LIMIT", "BUY", "NEW", LocalDateTime.now(), null)
        );

        // Available purchasing power: 10,000 - 3,000 = 7,000.00
        var purchasingPower = calculator.calculateAvailableFunds(orders);
        assertEquals(BigDecimal.valueOf(7000.00).setScale(2, RoundingMode.HALF_EVEN), purchasingPower);
    }
}
