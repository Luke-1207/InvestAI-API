package com.investai.api.module.relatorio.dto;

import java.math.BigDecimal;

public record RentabilidadePrazoDTO(
        String prazo,
        BigDecimal aliquotaIR,
        BigDecimal taxaBruta,
        BigDecimal taxaLiquida
) {
}
