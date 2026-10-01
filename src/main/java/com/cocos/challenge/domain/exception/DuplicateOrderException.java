package com.cocos.challenge.domain.exception;

public class DuplicateOrderException extends DomainException {
    public DuplicateOrderException(String idempotencyKey) {
        super("Order with idempotency key already processed: " + idempotencyKey);
    }
}
