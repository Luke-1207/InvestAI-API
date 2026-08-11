package com.investai.api.infra.hgbrasil.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class HgBrasilHistoricalPointDTO {
    private LocalDate data;
    private BigDecimal abertura;
    private BigDecimal fechamento;
    private BigDecimal maxima;
    private BigDecimal minima;
    private Long volume;
}