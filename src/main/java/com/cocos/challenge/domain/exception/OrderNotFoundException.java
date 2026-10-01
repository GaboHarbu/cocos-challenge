package com.cocos.challenge.domain.exception;

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
    }
}
