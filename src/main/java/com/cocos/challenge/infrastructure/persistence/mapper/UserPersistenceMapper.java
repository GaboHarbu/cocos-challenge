package com.cocos.challenge.infrastructure.persistence.mapper;

import com.cocos.challenge.domain.model.User;
import com.cocos.challenge.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new User(entity.getId(), entity.getEmail(), entity.getAccountNumber());
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }
        return new UserEntity(domain.id(), domain.email(), domain.accountNumber());
    }
}
