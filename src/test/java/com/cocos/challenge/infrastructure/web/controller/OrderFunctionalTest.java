package com.cocos.challenge.infrastructure.web.controller;

import com.cocos.challenge.application.dto.order.SendOrderRequest;
import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.entity.UserEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.repository.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MarketDataRepository marketDataRepository;

    private UserEntity testUser;
    private InstrumentEntity moneyInstrument;
    private InstrumentEntity ggalInstrument;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        marketDataRepository.deleteAll();
        instrumentRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create User
        testUser = userRepository.save(new UserEntity(null, "functional@cocos.com", "99001"));

        // 2. Create Money Instrument (ARS)
        moneyInstrument = instrumentRepository.save(new InstrumentEntity(null, "ARS", "PESOS", "MONEDA"));

        // 3. Create Stock Instrument (GGAL)
        ggalInstrument = instrumentRepository.save(new InstrumentEntity(null, "GGAL", "Grupo Financiero Galicia", "ACCIONES"));

        // 4. Create MarketData for GGAL: close = 100.00, previousClose = 95.00
        marketDataRepository.save(new MarketDataEntity(
                null,
                ggalInstrument.getId(),
                BigDecimal.valueOf(105.00),
                BigDecimal.valueOf(94.00),
                BigDecimal.valueOf(96.00),
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(95.00),
                LocalDate.now()
        ));

        // 5. Initial Cash Deposit: 500,000 ARS via CASH_IN order
        orderRepository.save(new OrderEntity(
                null,
                moneyInstrument.getId(),
                testUser.getId(),
                500000,
                BigDecimal.ONE,
                "MARKET",
                "CASH_IN",
                "FILLED",
                LocalDateTime.now(),
                null
        ));
    }

    @Test
    @DisplayName("Functional Test: Submit MARKET BUY order fills immediately and returns 201")
    void testSendMarketBuyOrder() throws Exception {
        var request = SendOrderRequest.builder()
                .userId(testUser.getId())
                .instrumentId(ggalInstrument.getId())
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(20)
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("FILLED")))
                .andExpect(jsonPath("$.type", is("MARKET")))
                .andExpect(jsonPath("$.side", is("BUY")))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.price", is(100.0)))
                .andExpect(jsonPath("$.totalValue", is(2000.0)))
                .andExpect(jsonPath("$.ticker", is("GGAL")));
    }

    @Test
    @DisplayName("Functional Test: Submit LIMIT BUY order creates order with status NEW")
    void testSendLimitBuyOrder() throws Exception {
        var request = SendOrderRequest.builder()
                .userId(testUser.getId())
                .ticker("GGAL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .size(10)
                .price(BigDecimal.valueOf(98.50))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("NEW")))
                .andExpect(jsonPath("$.type", is("LIMIT")))
                .andExpect(jsonPath("$.price", is(98.5)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalValue", is(985.0)));
    }

    @Test
    @DisplayName("Functional Test: Submit order by investment amount ($ ARS) calculates max whole shares")
    void testSendOrderByAmount() throws Exception {
        var request = SendOrderRequest.builder()
                .userId(testUser.getId())
                .ticker("GGAL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .amount(BigDecimal.valueOf(1050.00)) // at price 100 -> 10 shares
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FILLED")))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.price", is(100.0)))
                .andExpect(jsonPath("$.totalValue", is(1000.0)));
    }

    @Test
    @DisplayName("Functional Test: Submit order with insufficient cash returns REJECTED status")
    void testSendOrderInsufficientFundsRejects() throws Exception {
        var request = SendOrderRequest.builder()
                .userId(testUser.getId())
                .ticker("GGAL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(10000) // 10000 * 100 = 1,000,000 (Available cash is 500,000)
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("Functional Test: Idempotency-Key prevents double execution and returns identical response")
    void testIdempotencyKeySupport() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        var request = SendOrderRequest.builder()
                .userId(testUser.getId())
                .ticker("GGAL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(5)
                .build();

        // First call
        String firstResponse = mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FILLED")))
                .andReturn().getResponse().getContentAsString();

        Long firstOrderId = objectMapper.readTree(firstResponse).get("id").asLong();

        // Second call with same idempotency key
        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(firstOrderId.intValue())))
                .andExpect(jsonPath("$.status", is("FILLED")));
    }

    @Test
    @DisplayName("Functional Test: Cancel NEW order succeeds and returns status CANCELLED")
    void testCancelNewOrder() throws Exception {
        OrderEntity newOrder = orderRepository.save(new OrderEntity(
                null,
                ggalInstrument.getId(),
                testUser.getId(),
                15,
                BigDecimal.valueOf(90.00),
                "LIMIT",
                "BUY",
                "NEW",
                LocalDateTime.now(),
                null
        ));

        mockMvc.perform(post("/api/v1/orders/" + newOrder.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("Functional Test: Cancel FILLED order returns 422 Unprocessable Entity Problem Detail")
    void testCancelFilledOrderFails() throws Exception {
        OrderEntity filledOrder = orderRepository.save(new OrderEntity(
                null,
                ggalInstrument.getId(),
                testUser.getId(),
                15,
                BigDecimal.valueOf(100.00),
                "MARKET",
                "BUY",
                "FILLED",
                LocalDateTime.now(),
                null
        ));

        mockMvc.perform(post("/api/v1/orders/" + filledOrder.getId() + "/cancel"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type", is("urn:cocos:error:invalid-state")))
                .andExpect(jsonPath("$.title", is("Invalid Order State Transition")));
    }

    @Test
    @DisplayName("Functional Test: Portfolio endpoint returns correct cash and positions")
    void testGetPortfolio() throws Exception {
        // Buy 50 shares of GGAL
        orderRepository.save(new OrderEntity(
                null,
                ggalInstrument.getId(),
                testUser.getId(),
                50,
                BigDecimal.valueOf(100.00),
                "MARKET",
                "BUY",
                "FILLED",
                LocalDateTime.now(),
                null
        ));

        mockMvc.perform(get("/api/v1/portfolio").param("userId", testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.accountNumber", is("99001")))
                .andExpect(jsonPath("$.totalCash", is(495000.0))) // 500,000 - 5,000
                .andExpect(jsonPath("$.availableCash", is(495000.0)))
                .andExpect(jsonPath("$.totalAccountValue", is(500000.0))) // 495,000 cash + 5,000 stock
                .andExpect(jsonPath("$.positions[0].ticker", is("GGAL")))
                .andExpect(jsonPath("$.positions[0].size", is(50)))
                .andExpect(jsonPath("$.positions[0].marketPrice", is(100.0)))
                .andExpect(jsonPath("$.positions[0].marketValue", is(5000.0)));
    }

    @Test
    @DisplayName("Functional Test: Search instruments returns matching items with pagination")
    void testSearchInstruments() throws Exception {
        mockMvc.perform(get("/api/v1/instruments").param("query", "galicia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticker", is("GGAL")))
                .andExpect(jsonPath("$.content[0].name", is("Grupo Financiero Galicia")))
                .andExpect(jsonPath("$.content[0].latestPrice", is(100.0)));
    }

    @Test
    @DisplayName("Functional Test: Malformed JSON request returns 400 Bad Request Problem Detail")
    void testMalformedJsonReturnsBadRequest() throws Exception {
        String malformedJson = "{\"userId\": \"not-a-number\", \"ticker\": \"GGAL\"}";

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.type", is("urn:cocos:error:malformed-request")))
                .andExpect(jsonPath("$.title", is("Malformed Request")))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("Functional Test: Trace ID filter always generates and returns a 16-char trace ID")
    void testTraceIdValidationFilter() throws Exception {
        mockMvc.perform(get("/api/v1/instruments"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(header().string("X-Trace-Id", org.hamcrest.Matchers.matchesRegex("^[a-fA-F0-9]{16}$")));
    }
}
