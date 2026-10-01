package com.cocos.challenge.application.service;

import com.cocos.challenge.application.dto.instrument.InstrumentDto;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.repository.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.repository.MarketDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private MarketDataRepository marketDataRepository;

    private InstrumentService instrumentService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        instrumentService = new InstrumentService(instrumentRepository, marketDataRepository, new com.cocos.challenge.application.mapper.InstrumentMapper());
    }

    @Test
    @DisplayName("Search returns paginated InstrumentDto with latest price and return")
    void testSearchInstruments() {
        var ggal = new InstrumentEntity(1L, "GGAL", "Grupo Financiero Galicia", "ACCIONES");
        Pageable pageable = PageRequest.of(0, 10);
        Page<InstrumentEntity> instrumentPage = new PageImpl<>(List.of(ggal), pageable, 1);

        var md = new MarketDataEntity(10L, 1L, BigDecimal.valueOf(110), BigDecimal.valueOf(95),
                BigDecimal.valueOf(100), BigDecimal.valueOf(105.00), BigDecimal.valueOf(100.00), LocalDate.now());

        when(instrumentRepository.findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase("GGAL", "GGAL", pageable))
                .thenReturn(instrumentPage);
        when(marketDataRepository.findByInstrumentIdInOrderByDateDescIdDesc(Set.of(1L)))
                .thenReturn(List.of(md));

        Page<InstrumentDto> result = instrumentService.search("GGAL", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        InstrumentDto dto = result.getContent().get(0);
        assertEquals("GGAL", dto.ticker());
        assertEquals(BigDecimal.valueOf(105.00), dto.latestPrice());
        assertEquals(BigDecimal.valueOf(5.00).setScale(2), dto.dailyReturnPercent());

        verify(instrumentRepository).findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase("GGAL", "GGAL", pageable);
    }
}
