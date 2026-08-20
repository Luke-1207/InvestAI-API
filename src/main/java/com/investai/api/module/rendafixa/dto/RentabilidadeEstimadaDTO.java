package com.investai.api.module.rendafixa.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RentabilidadeEstimadaDTO {
    private BigDecimal taxaBrutaAnual;
    private BigDecimal aliquotaIR;
    private BigDecimal taxaLiquidaAnual;
}