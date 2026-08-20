package com.investai.api.module.rendafixa.dto;

import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TituloPrivadoDetalheResponseDTO {
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
    private RentabilidadeEstimadaDTO rentabilidadeEstimada;
    private String resumoIA;
}