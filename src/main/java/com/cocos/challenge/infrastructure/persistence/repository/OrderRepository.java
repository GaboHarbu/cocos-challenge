package com.cocos.challenge.infrastructure.persistence.repository;

import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    List<OrderEntity> findByUserIdOrderByDatetimeAsc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OrderEntity> getByUserIdOrderByDatetimeAsc(Long userId);
}
