package com.cocos.challenge.application.service;

import com.cocos.challenge.application.dto.order.OrderResponse;
import com.cocos.challenge.application.dto.order.SendOrderRequest;
import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.exception.InstrumentNotFoundException;
import com.cocos.challenge.domain.exception.InvalidOrderException;
import com.cocos.challenge.domain.exception.InvalidOrderStateException;
import com.cocos.challenge.domain.exception.OrderNotFoundException;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.entity.UserEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.repository.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MarketDataRepository marketDataRepository;

    private OrderService orderService;

    private InstrumentEntity sampleInstrument;
    private MarketDataEntity sampleMarketData;
    private UserEntity sampleUser;

    @BeforeEach
    void setUp() {
        sampleInstrument = new InstrumentEntity(10L, "GGAL", "Grupo Financiero Galicia", "ACCIONES");
        sampleMarketData = new MarketDataEntity(1L, 10L, BigDecimal.valueOf(105), BigDecimal.valueOf(95),
                BigDecimal.valueOf(98), BigDecimal.valueOf(100.00), BigDecimal.valueOf(95.00), LocalDate.now());
        sampleUser = new UserEntity(1L, "user@test.com", "12345");

        var cashCalc = new com.cocos.challenge.application.calculator.CashCalculator();
        var holdingCalc = new com.cocos.challenge.application.calculator.HoldingCalculator();
        var priceResolver = new com.cocos.challenge.application.resolver.OrderPriceResolver(marketDataRepository);
        var validator = new com.cocos.challenge.application.validator.OrderValidator(cashCalc, holdingCalc);
        var mapper = new com.cocos.challenge.application.mapper.OrderMapper();

        orderService = new OrderService(orderRepository, instrumentRepository, userRepository, priceResolver, validator, mapper);
    }

    @Test
    @DisplayName("Throws InstrumentNotFoundException when instrument does not exist")
    void testInstrumentNotFound() {
        when(instrumentRepository.findById(999L)).thenReturn(Optional.empty());

        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(999L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(10)
                .build();

        assertThrows(InstrumentNotFoundException.class, () -> orderService.sendOrder(request, "key-1"));
    }

    @Test
    @DisplayName("BUY order is REJECTED when available funds are insufficient")
    void testBuyOrderRejectedWhenInsufficientFunds() {
        when(instrumentRepository.findById(10L)).thenReturn(Optional.of(sampleInstrument));
        when(marketDataRepository.findFirstByInstrumentIdOrderByDateDescIdDesc(10L)).thenReturn(Optional.of(sampleMarketData));
        when(userRepository.getUserById(1L)).thenReturn(Optional.of(sampleUser));

        // Available funds: $500, required for 10 shares @ $100 = $1,000
        var cashOrder = new OrderEntity(1L, 99L, 1L, 500, BigDecimal.ONE, "MARKET", "CASH_IN", "FILLED", LocalDateTime.now(), null);
        when(orderRepository.getByUserIdOrderByDatetimeAsc(1L)).thenReturn(List.of(cashOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(10L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(10)
                .build();

        OrderResponse result = orderService.sendOrder(request, "key-2");

        assertNotNull(result);
        assertEquals(OrderStatus.REJECTED, result.status());
        assertNotNull(result.rejectionReason());
        assertTrue(result.rejectionReason().contains("Insufficient funds"));
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("SELL order is REJECTED when available shares are insufficient")
    void testSellOrderRejectedWhenInsufficientShares() {
        when(instrumentRepository.findById(10L)).thenReturn(Optional.of(sampleInstrument));
        when(marketDataRepository.findFirstByInstrumentIdOrderByDateDescIdDesc(10L)).thenReturn(Optional.of(sampleMarketData));
        when(userRepository.getUserById(1L)).thenReturn(Optional.of(sampleUser));

        // Available shares: 5, trying to sell 10
        var buyOrder = new OrderEntity(1L, 10L, 1L, 5, BigDecimal.valueOf(100), "MARKET", "BUY", "FILLED", LocalDateTime.now(), null);
        when(orderRepository.getByUserIdOrderByDatetimeAsc(1L)).thenReturn(List.of(buyOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(10L)
                .side(OrderSide.SELL)
                .type(OrderType.MARKET)
                .size(10)
                .build();

        OrderResponse result = orderService.sendOrder(request, "key-3");

        assertNotNull(result);
        assertEquals(OrderStatus.REJECTED, result.status());
        assertNotNull(result.rejectionReason());
        assertTrue(result.rejectionReason().contains("Insufficient shares"));
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Market BUY order succeeds and persists with FILLED status when funds are sufficient")
    void testBuyOrderSuccess() {
        when(instrumentRepository.findById(10L)).thenReturn(Optional.of(sampleInstrument));
        when(marketDataRepository.findFirstByInstrumentIdOrderByDateDescIdDesc(10L)).thenReturn(Optional.of(sampleMarketData));
        when(userRepository.getUserById(1L)).thenReturn(Optional.of(sampleUser));

        // Available funds: $5,000, required for 10 shares @ $100 = $1,000
        var cashOrder = new OrderEntity(1L, 99L, 1L, 5000, BigDecimal.ONE, "MARKET", "CASH_IN", "FILLED", LocalDateTime.now(), null);
        when(orderRepository.getByUserIdOrderByDatetimeAsc(1L)).thenReturn(List.of(cashOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity o = inv.getArgument(0);
            o.setId(42L);
            return o;
        });

        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(10L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(10)
                .build();

        OrderResponse result = orderService.sendOrder(request, "key-4");

        assertNotNull(result);
        assertEquals(42L, result.orderId());
        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), result.price());
        assertEquals(BigDecimal.valueOf(1000.00).setScale(2), result.totalValue());
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Idempotent replay returns previously created order")
    void testIdempotentReplay() {
        var existingOrder = new OrderEntity(100L, 10L, 1L, 10, BigDecimal.valueOf(100), "MARKET", "BUY", "FILLED", LocalDateTime.now(), "key-idem");
        when(orderRepository.findByIdempotencyKey("key-idem")).thenReturn(Optional.of(existingOrder));
        when(instrumentRepository.findById(10L)).thenReturn(Optional.of(sampleInstrument));

        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(10L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(10)
                .build();

        OrderResponse result = orderService.sendOrder(request, "key-idem");

        assertNotNull(result);
        assertEquals(100L, result.id());
        assertEquals(OrderStatus.FILLED, result.status());
        assertTrue(result.message().contains("idempotent"));
    }

    @Test
    @DisplayName("Idempotent replay throws InvalidOrderException if key belongs to another user")
    void testIdempotentReplayDifferentUserThrowsException() {
        var existingOrder = new OrderEntity(100L, 10L, 2L, 10, BigDecimal.valueOf(100), "MARKET", "BUY", "FILLED", LocalDateTime.now(), "key-other-user");
        when(orderRepository.findByIdempotencyKey("key-other-user")).thenReturn(Optional.of(existingOrder));

        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(10L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(10)
                .build();

        var ex = assertThrows(InvalidOrderException.class, () -> orderService.sendOrder(request, "key-other-user"));
        assertTrue(ex.getMessage().contains("different user"));
    }

    @Test
    @DisplayName("Idempotent replay throws InvalidOrderException if parameters conflict with original order")
    void testIdempotentReplayConflictingSideThrowsException() {
        var existingOrder = new OrderEntity(100L, 10L, 1L, 10, BigDecimal.valueOf(100), "MARKET", "BUY", "FILLED", LocalDateTime.now(), "key-conflict");
        when(orderRepository.findByIdempotencyKey("key-conflict")).thenReturn(Optional.of(existingOrder));

        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(10L)
                .side(OrderSide.SELL) // Changed from BUY to SELL
                .type(OrderType.MARKET)
                .size(10)
                .build();

        var ex = assertThrows(InvalidOrderException.class, () -> orderService.sendOrder(request, "key-conflict"));
        assertTrue(ex.getMessage().contains("different order type or side"));
    }

    @Test
    @DisplayName("Throws InvalidOrderException if idempotency key is blank")
    void testBlankIdempotencyKeyThrowsException() {
        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(10L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(10)
                .build();

        var ex = assertThrows(InvalidOrderException.class, () -> orderService.sendOrder(request, "   "));
        assertTrue(ex.getMessage().contains("cannot be blank"));
    }

    @Test
    @DisplayName("Throws InvalidOrderException if idempotency key contains invalid characters")
    void testMalformedIdempotencyKeyThrowsException() {
        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(10L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(10)
                .build();

        var ex = assertThrows(InvalidOrderException.class, () -> orderService.sendOrder(request, "bad key!@#"));
        assertTrue(ex.getMessage().contains("invalid characters"));
    }

    @Test
    @DisplayName("Order by investment amount calculates max whole shares")
    void testOrderByAmountCalculation() {
        when(instrumentRepository.findById(10L)).thenReturn(Optional.of(sampleInstrument));
        when(marketDataRepository.findFirstByInstrumentIdOrderByDateDescIdDesc(10L)).thenReturn(Optional.of(sampleMarketData));
        when(userRepository.getUserById(1L)).thenReturn(Optional.of(sampleUser));

        var cashOrder = new OrderEntity(1L, 99L, 1L, 50000, BigDecimal.ONE, "MARKET", "CASH_IN", "FILLED", LocalDateTime.now(), null);
        when(orderRepository.getByUserIdOrderByDatetimeAsc(1L)).thenReturn(List.of(cashOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity o = inv.getArgument(0);
            o.setId(88L);
            return o;
        });

        // Amount $1050 / $100/share = 10 shares
        SendOrderRequest request = SendOrderRequest.builder()
                .userId(1L)
                .instrumentId(10L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .amount(BigDecimal.valueOf(1050.00))
                .build();

        OrderResponse result = orderService.sendOrder(request, "key-amount");

        assertNotNull(result);
        assertEquals(10, result.size());
        assertEquals(BigDecimal.valueOf(1000.00).setScale(2), result.totalValue());
    }

    @Test
    @DisplayName("Successfully cancels order when status is NEW")
    void testCancelNewOrderSuccess() {
        var newOrder = new OrderEntity(200L, 10L, 1L, 15, BigDecimal.valueOf(90), "LIMIT", "BUY", "NEW", LocalDateTime.now(), null);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(newOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(instrumentRepository.findById(10L)).thenReturn(Optional.of(sampleInstrument));

        OrderResponse response = orderService.cancelOrder(200L);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals("Order cancelled successfully.", response.message());
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Fails to cancel order when status is already FILLED")
    void testCancelFilledOrderFails() {
        var filledOrder = new OrderEntity(201L, 10L, 1L, 15, BigDecimal.valueOf(100), "MARKET", "BUY", "FILLED", LocalDateTime.now(), null);
        when(orderRepository.findById(201L)).thenReturn(Optional.of(filledOrder));

        assertThrows(InvalidOrderStateException.class, () -> orderService.cancelOrder(201L));
    }

    @Test
    @DisplayName("Throws OrderNotFoundException when order to cancel does not exist")
    void testCancelOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder(999L));
    }
}
