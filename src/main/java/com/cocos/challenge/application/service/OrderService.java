package com.cocos.challenge.application.service;

import com.cocos.challenge.application.dto.order.OrderResponse;
import com.cocos.challenge.application.dto.order.SendOrderRequest;
import com.cocos.challenge.application.mapper.OrderMapper;
import com.cocos.challenge.application.resolver.OrderPriceResolver;
import com.cocos.challenge.application.validator.OrderValidator;
import com.cocos.challenge.domain.enums.OrderStatus;
import com.cocos.challenge.domain.exception.InstrumentNotFoundException;
import com.cocos.challenge.domain.exception.InvalidOrderException;
import com.cocos.challenge.domain.exception.InvalidOrderStateException;
import com.cocos.challenge.domain.exception.OrderNotFoundException;
import com.cocos.challenge.domain.exception.UserNotFoundException;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final UserRepository userRepository;
    private final OrderPriceResolver orderPriceResolver;
    private final OrderValidator orderValidator;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse sendOrder(SendOrderRequest request, String idempotencyKey) {
        orderValidator.validateIdempotencyKey(idempotencyKey);

        // 1. Fast-path: Check if already processed (avoids lock if it's a simple client retry)
        var idempotent = checkIdempotency(idempotencyKey, request);
        if (idempotent.isPresent()) {
            return idempotent.get();
        }

        // 2. Validate instrument and basic inputs before acquiring DB locks
        var instrument = resolveInstrument(request);
        orderValidator.validateNotCurrency(instrument);

        var executionPrice = orderPriceResolver.resolvePrice(request, instrument);
        int size = resolveOrderSize(request, executionPrice);
        log.info("[TRADING] [ORDER_SUBMITTED] userId={} ticker={} side={} type={} size={} amount={} ARS price={} ARS",
                request.userId(), instrument.getTicker(), request.side(), request.type(), size, request.amount(), executionPrice);

        if (!hasExecutableShares(size)) {
            return rejectInsufficientAmount(request, instrument, executionPrice, idempotencyKey);
        }

        // 3. Lock user to serialize concurrent orders from this user and prevent double-spending
        lockUserForUpdate(request.userId());

        // 4. Double-check: in case a concurrent request with the same idempotency key was committed while waiting for lock
        if (StringUtils.hasText(idempotencyKey)) {
            var recheck = checkIdempotency(idempotencyKey, request);
            if (recheck.isPresent()) {
                return recheck.get();
            }
        }

        // 5. Process execution with validated funds/shares
        return processOrderExecution(request, instrument, executionPrice, size, idempotencyKey);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.isNew()) {
            throw new InvalidOrderStateException("Only orders in NEW status can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        var saved = orderRepository.save(order);
        var instrument = instrumentRepository.findById(saved.getInstrumentId()).orElse(null);
        log.info("[TRADING] [ORDER_CANCELLED] orderId={} userId={} ticker={} side={} size={} price={} ARS",
                saved.getId(), saved.getUserId(), instrument != null ? instrument.getTicker() : "N/A", saved.getSide(), saved.getSize(), saved.getPrice());

        return orderMapper.toResponse(saved, instrument, OrderStatus.CANCELLED, null, "Order cancelled successfully.");
    }

    private Optional<OrderResponse> checkIdempotency(String idempotencyKey, SendOrderRequest request) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return Optional.empty();
        }
        return orderRepository.findByIdempotencyKey(idempotencyKey.trim())
                .map(existingOrder -> {
                    var instrument = instrumentRepository.findById(existingOrder.getInstrumentId()).orElse(null);
                    orderValidator.validateIdempotentMatches(existingOrder, request, instrument);
                    log.info("[AUDIT] [IDEMPOTENCY_REPLAY] key={} userId={} orderId={} status={}",
                            idempotencyKey, existingOrder.getUserId(), existingOrder.getId(), existingOrder.getStatus());
                    return orderMapper.toResponse(existingOrder, instrument, existingOrder.getStatus(), null, "Order already processed (idempotent response).");
                });
    }

    private int resolveOrderSize(SendOrderRequest request, BigDecimal executionPrice) {
        if (request.hasSize()) {
            return request.size();
        }
        if (request.hasAmount()) {
            return request.amount().divide(executionPrice, 0, RoundingMode.FLOOR).intValue();
        }
        throw new InvalidOrderException("Either size or amount must be specified.");
    }

    private OrderResponse rejectInsufficientAmount(SendOrderRequest request, InstrumentEntity instrument, BigDecimal executionPrice, String idempotencyKey) {
        var rejectionReason = "Amount sent ($" + request.amount() + ") is insufficient to buy at least 1 share at price $" + executionPrice;
        var rejectedEntity = persistOrder(request, instrument.getId(), 0, executionPrice, OrderStatus.REJECTED, idempotencyKey);
        log.warn("[TRADING] [ORDER_REJECTED] userId={} ticker={} side={} amount={} ARS price={} ARS reason=\"{}\"",
                request.userId(), instrument.getTicker(), request.side(), request.amount(), executionPrice, rejectionReason);
        return orderMapper.toResponse(rejectedEntity, instrument, OrderStatus.REJECTED, rejectionReason, "Rejected: " + rejectionReason);
    }

    private void lockUserForUpdate(Long userId) {
        userRepository.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private OrderResponse processOrderExecution(SendOrderRequest request, InstrumentEntity instrument, BigDecimal executionPrice, int size, String idempotencyKey) {
        var userOrders = orderRepository.getByUserIdOrderByDatetimeAsc(request.userId());
        var validation = orderValidator.validateFundsAndShares(request, executionPrice, size, userOrders, instrument.getId());

        var savedOrder = persistOrder(request, instrument.getId(), size, executionPrice, validation.status(), idempotencyKey);

        if (validation.status() == OrderStatus.REJECTED) {
            log.warn("[TRADING] [ORDER_REJECTED] orderId={} userId={} ticker={} side={} size={} price={} ARS reason=\"{}\"",
                    savedOrder.getId(), request.userId(), instrument.getTicker(), request.side(), size, executionPrice, validation.rejectionReason());
        } else if (validation.status() == OrderStatus.FILLED) {
            log.info("[TRADING] [ORDER_FILLED] orderId={} userId={} ticker={} side={} size={} price={} ARS total={} ARS",
                    savedOrder.getId(), request.userId(), instrument.getTicker(), request.side(), size, executionPrice, savedOrder.totalValue());
        } else {
            log.info("[TRADING] [ORDER_NEW] orderId={} userId={} ticker={} side={} size={} limitPrice={} ARS committed={} ARS",
                    savedOrder.getId(), request.userId(), instrument.getTicker(), request.side(), size, executionPrice, savedOrder.totalValue());
        }

        var message = validation.status() == OrderStatus.REJECTED
                ? "Rejected: " + validation.rejectionReason()
                : (validation.status() == OrderStatus.FILLED ? "Market order executed successfully (FILLED)." : "Limit order created successfully (NEW).");

        return orderMapper.toResponse(savedOrder, instrument, validation.status(), validation.rejectionReason(), message);
    }

    private OrderEntity persistOrder(SendOrderRequest request, Long instrumentId, int size, BigDecimal price, OrderStatus status, String idempotencyKey) {
        var entity = OrderEntity.builder()
                .instrumentId(instrumentId)
                .userId(request.userId())
                .size(size)
                .price(price)
                .type(request.type())
                .side(request.side())
                .status(status)
                .datetime(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .build();
        return orderRepository.save(entity);
    }

    private InstrumentEntity resolveInstrument(SendOrderRequest request) {
        if (request.hasInstrumentId()) {
            return instrumentRepository.findById(request.instrumentId())
                    .orElseThrow(() -> new InstrumentNotFoundException(request.instrumentId()));
        }
        if (request.hasTicker()) {
            return instrumentRepository.findByTickerIgnoreCase(request.ticker().trim())
                    .orElseThrow(() -> new InstrumentNotFoundException(request.ticker().trim()));
        }
        throw new InvalidOrderException("Either instrumentId or ticker must be provided.");
    }

    private boolean hasExecutableShares(int size) {
        return size > 0;
    }
}
