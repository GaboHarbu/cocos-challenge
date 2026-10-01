package com.cocos.challenge.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "marketdata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrumentid", nullable = false)
    private Long instrumentId;

    @Column(name = "high", precision = 10, scale = 2)
    private BigDecimal high;

    @Column(name = "low", precision = 10, scale = 2)
    private BigDecimal low;

    @Column(name = "open", precision = 10, scale = 2)
    private BigDecimal open;

    @Column(name = "close", precision = 10, scale = 2)
    private BigDecimal close;

    @Column(name = "previousclose", precision = 10, scale = 2)
    private BigDecimal previousClose;

    @Column(name = "date", nullable = false)
    private LocalDate date;
}
