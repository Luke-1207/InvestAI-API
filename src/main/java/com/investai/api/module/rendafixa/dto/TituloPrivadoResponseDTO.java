package com.investai.api.module.rendafixa.dto;

import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TituloPrivadoResponseDTO {
    private UUID id;
    private TipoTituloPrivado tipo;
    private String emissor;
    private Indexador indexador;
    private BigDecimal taxaPercentual;
    private LocalDate vencimento;
    private BigDecimal investimentoMinimo;
    private TipoLiquidez liquidez;
    private boolean garantidoFgc;
    private boolean isentoIr;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}