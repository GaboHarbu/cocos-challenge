package com.cocos.challenge.infrastructure.persistence.entity;

import com.cocos.challenge.domain.enums.InstrumentType;
import com.cocos.challenge.domain.model.Instrument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, length = 10)
    private String ticker;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private InstrumentType type;

    public InstrumentEntity(Long id, String ticker, String name, String type) {
        this(id, ticker, name, type != null ? InstrumentType.valueOf(type.toUpperCase()) : InstrumentType.ACCIONES);
    }

    public boolean isMoney() {
        return type == InstrumentType.MONEDA || Instrument.CURRENCY_TICKER_ARS.equalsIgnoreCase(ticker);
    }
}
