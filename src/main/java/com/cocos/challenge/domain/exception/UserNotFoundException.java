package com.cocos.challenge.domain.exception;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(Long userId) {
        super("User not found with id: " + userId);
    }
}
