package com.cocos.challenge.infrastructure.persistence.mapper;

import com.cocos.challenge.domain.enums.InstrumentType;
import com.cocos.challenge.domain.enums.OrderSide;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.enums.OrderType;
import com.cocos.challenge.domain.model.Instrument;
import com.cocos.challenge.domain.model.MarketData;
import com.cocos.challenge.domain.model.Order;
import com.cocos.challenge.domain.model.User;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersistenceMapperTest {

    private final UserPersistenceMapper userMapper = new UserPersistenceMapper();
    private final InstrumentPersistenceMapper instrumentMapper = new InstrumentPersistenceMapper();
    private final MarketDataPersistenceMapper marketDataMapper = new MarketDataPersistenceMapper();
    private final OrderPersistenceMapper orderMapper = new OrderPersistenceMapper();

    @Test
    @DisplayName("UserPersistenceMapper: bidirectional mapping and null handling")
    void testUserMapper() {
        assertNull(userMapper.toDomain(null));
        assertNull(userMapper.toEntity(null));

        UserEntity entity = new UserEntity(1L, "test@cocos.com", "10001");
        User domain = userMapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(1L, domain.id());
        assertEquals("test@cocos.com", domain.email());
        assertEquals("10001", domain.accountNumber());

        UserEntity mappedEntity = userMapper.toEntity(domain);
        assertNotNull(mappedEntity);
        assertEquals(domain.id(), mappedEntity.getId());
        assertEquals(domain.email(), mappedEntity.getEmail());
        assertEquals(domain.accountNumber(), mappedEntity.getAccountNumber());
    }

    @Test
    @DisplayName("InstrumentPersistenceMapper: bidirectional mapping and null handling")
    void testInstrumentMapper() {
        assertNull(instrumentMapper.toDomain(null));
        assertNull(instrumentMapper.toEntity(null));

        InstrumentEntity entity = new InstrumentEntity(10L, "GGAL", "Grupo Galicia", InstrumentType.ACCIONES);
        Instrument domain = instrumentMapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(10L, domain.id());
        assertEquals("GGAL", domain.ticker());
        assertEquals("Grupo Galicia", domain.name());
        assertEquals(InstrumentType.ACCIONES, domain.type());

        InstrumentEntity mappedEntity = instrumentMapper.toEntity(domain);
        assertNotNull(mappedEntity);
        assertEquals(domain.id(), mappedEntity.getId());
        assertEquals(domain.ticker(), mappedEntity.getTicker());
        assertEquals(domain.name(), mappedEntity.getName());
        assertEquals(domain.type(), mappedEntity.getType());
    }

    @Test
    @DisplayName("MarketDataPersistenceMapper: bidirectional mapping and null handling")
    void testMarketDataMapper() {
        assertNull(marketDataMapper.toDomain(null));
        assertNull(marketDataMapper.toEntity(null));

        LocalDate now = LocalDate.now();
        MarketDataEntity entity = new MarketDataEntity(
                5L,
                10L,
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(95),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(100),
                now
        );

        MarketData domain = marketDataMapper.toDomain(entity);
        assertNotNull(domain);
        assertEquals(5L, domain.id());
        assertEquals(10L, domain.instrumentId());
        assertEquals(BigDecimal.valueOf(105), domain.close());
        assertEquals(BigDecimal.valueOf(100), domain.previousClose());
        assertEquals(now, domain.date());

        MarketDataEntity mappedEntity = marketDataMapper.toEntity(domain);
        assertNotNull(mappedEntity);
        assertEquals(domain.id(), mappedEntity.getId());
        assertEquals(domain.instrumentId(), mappedEntity.getInstrumentId());
        assertEquals(domain.close(), mappedEntity.getClose());
        assertEquals(domain.previousClose(), mappedEntity.getPreviousClose());
        assertEquals(domain.date(), mappedEntity.getDate());
    }

    @Test
    @DisplayName("OrderPersistenceMapper: bidirectional mapping and null handling")
    void testOrderMapper() {
        assertNull(orderMapper.toDomain(null));
        assertNull(orderMapper.toEntity(null));

        LocalDateTime now = LocalDateTime.now();
        OrderEntity entity = OrderEntity.builder()
                .id(20L)
                .instrumentId(10L)
                .userId(1L)
                .size(50)
                .price(BigDecimal.valueOf(95.50))
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .status(OrderStatus.NEW)
                .datetime(now)
                .idempotencyKey("test-uuid")
                .build();

        Order domain = orderMapper.toDomain(entity);
        assertNotNull(domain);
        assertEquals(20L, domain.getId());
        assertEquals(10L, domain.getInstrumentId());
        assertEquals(1L, domain.getUserId());
        assertEquals(50, domain.getSize());
        assertEquals(BigDecimal.valueOf(95.50), domain.getPrice());
        assertEquals(OrderType.LIMIT, domain.getType());
        assertEquals(OrderSide.BUY, domain.getSide());
        assertEquals(OrderStatus.NEW, domain.getStatus());
        assertEquals(now, domain.getDatetime());
        assertEquals("test-uuid", domain.getIdempotencyKey());

        OrderEntity mappedEntity = orderMapper.toEntity(domain);
        assertNotNull(mappedEntity);
        assertEquals(domain.getId(), mappedEntity.getId());
        assertEquals(domain.getInstrumentId(), mappedEntity.getInstrumentId());
        assertEquals(domain.getUserId(), mappedEntity.getUserId());
        assertEquals(domain.getSize(), mappedEntity.getSize());
        assertEquals(domain.getPrice(), mappedEntity.getPrice());
        assertEquals(domain.getType(), mappedEntity.getType());
        assertEquals(domain.getSide(), mappedEntity.getSide());
        assertEquals(domain.getStatus(), mappedEntity.getStatus());
        assertEquals(domain.getDatetime(), mappedEntity.getDatetime());
        assertEquals(domain.getIdempotencyKey(), mappedEntity.getIdempotencyKey());
    }
}
