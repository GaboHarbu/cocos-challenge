package com.cocos.challenge.infrastructure.persistence.repository;

import com.cocos.challenge.domain.enums.InstrumentType;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {

    Optional<InstrumentEntity> findByTickerIgnoreCase(String ticker);

    Page<InstrumentEntity> findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(String ticker, String name, Pageable pageable);

    Optional<InstrumentEntity> findFirstByType(InstrumentType type);
}
