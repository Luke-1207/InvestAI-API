package com.investai.api.module.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class IndicadoresMercadoResponseDTO {
    private BigDecimal ibovespaPontos;
    private BigDecimal ibovespaVariacaoDia;
    private BigDecimal dolarValor;
    private BigDecimal dolarVariacaoDia;
    private BigDecimal euroValor;
    private BigDecimal euroVariacaoDia;
    private BigDecimal selicAtual;
    private BigDecimal ipcaAcumulado12m;
    private LocalDateTime sincronizadoEm;
    private LocalDateTime selicIpcaSincronizadoEm;
}