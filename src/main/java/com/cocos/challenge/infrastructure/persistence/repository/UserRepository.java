package com.cocos.challenge.infrastructure.persistence.repository;

import com.cocos.challenge.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Acquires a pessimistic write lock (SELECT ... FOR UPDATE) on the user
     * to prevent race conditions during order placement and balance verification.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserEntity> getUserById(Long id);
}
