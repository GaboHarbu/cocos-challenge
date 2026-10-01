package com.cocos.challenge.infrastructure.persistence.repository;

import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketDataEntity, Long> {

    Optional<MarketDataEntity> findFirstByInstrumentIdOrderByDateDescIdDesc(Long instrumentId);

    List<MarketDataEntity> findByInstrumentIdInOrderByDateDescIdDesc(Collection<Long> instrumentIds);
}
