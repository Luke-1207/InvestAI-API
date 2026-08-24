package com.investai.api.infra.hgbrasil.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IndicadoresMercadoExternoDTO {
    private BigDecimal ibovespaPontos;
    private BigDecimal ibovespaVariacaoDia;
    private BigDecimal dolarValor;
    private BigDecimal dolarVariacaoDia;
    private BigDecimal euroValor;
    private BigDecimal euroVariacaoDia;
}