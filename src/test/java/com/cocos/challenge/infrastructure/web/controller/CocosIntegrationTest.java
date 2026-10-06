package com.cocos.challenge.infrastructure.web.controller;

import com.cocos.challenge.application.dto.order.SendOrderRequest;
import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("resource")
public class CocosIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("E2E Test: Get Portfolio for User 1 returns calculated total value, cash and positions")
    void testGetPortfolioUser1() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.accountNumber", is("10001")))
                .andExpect(jsonPath("$.totalCash", notNullValue()))
                .andExpect(jsonPath("$.availableCash", notNullValue()))
                .andExpect(jsonPath("$.totalAccountValue", notNullValue()))
                .andExpect(jsonPath("$.positions", notNullValue()));
    }

    @Test
    @DisplayName("E2E Test: Search Instruments by ticker and name with pagination")
    void testSearchInstruments() throws Exception {
        // Search by ticker
        mockMvc.perform(get("/api/v1/instruments").param("query", "GGAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].ticker", is("GGAL")))
                .andExpect(jsonPath("$.content[0].latestPrice", notNullValue()));

        // Search by company name
        mockMvc.perform(get("/api/v1/instruments").param("query", "Galicia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].ticker", is("GGAL")));
    }

    @Test
    @DisplayName("E2E Test: Send MARKET BUY order executes immediately with status FILLED")
    void testSendMarketBuyOrder() throws Exception {
        var request = SendOrderRequest.builder()
                .userId(1L)
                .ticker("GGAL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(10)
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
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.ticker", is("GGAL")));
    }

    @Test
    @DisplayName("E2E Test: Send LIMIT BUY order registers with status NEW")
    void testSendLimitBuyOrder() throws Exception {
        var request = SendOrderRequest.builder()
                .userId(1L)
                .ticker("GGAL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .size(5)
                .price(BigDecimal.valueOf(850.00))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("NEW")))
                .andExpect(jsonPath("$.type", is("LIMIT")))
                .andExpect(jsonPath("$.price", is(850.0)));
    }

    @Test
    @DisplayName("E2E Test: Send order specifying investment amount in ARS calculates whole shares")
    void testSendOrderByAmount() throws Exception {
        var request = SendOrderRequest.builder()
                .userId(1L)
                .ticker("GGAL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(BigDecimal.valueOf(100.00))
                .amount(BigDecimal.valueOf(550.00)) // 550 / 100 = 5 shares
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.totalValue", is(500.0)));
    }

    @Test
    @DisplayName("E2E Test: Send order with excessive amount is REJECTED and saved")
    void testSendOrderInsufficientFundsRejects() throws Exception {
        var request = SendOrderRequest.builder()
                .userId(1L)
                .ticker("GGAL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(50000) // 50,000 shares * price exceeds user cash
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("E2E Test: Idempotency-Key prevents duplicate orders under real PostgreSQL unique constraint")
    void testIdempotencyKeyRealPostgres() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        var request = SendOrderRequest.builder()
                .userId(1L)
                .ticker("GGAL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .size(2)
                .build();

        // 1st request
        String firstResponse = mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FILLED")))
                .andReturn().getResponse().getContentAsString();

        Long firstOrderId = objectMapper.readTree(firstResponse).get("id").asLong();

        // 2nd request (replay)
        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(firstOrderId.intValue())))
                .andExpect(jsonPath("$.status", is("FILLED")));
    }

    @Test
    @DisplayName("E2E Test: Cancel order 5 (NEW status in seed data) succeeds")
    void testCancelNewOrderFromSeed() throws Exception {
        // Order 5 is in status NEW in seed data
        mockMvc.perform(post("/api/v1/orders/5/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("E2E Test: Cancel order 2 (FILLED status in seed data) fails with RFC 7807 problem detail")
    void testCancelFilledOrderFails() throws Exception {
        // Order 2 is FILLED in seed data
        mockMvc.perform(post("/api/v1/orders/2/cancel"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type", is("urn:cocos:error:invalid-state")))
                .andExpect(jsonPath("$.title", is("Invalid Order State Transition")));
    }
}
