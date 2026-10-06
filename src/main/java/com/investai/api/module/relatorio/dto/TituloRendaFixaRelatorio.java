package com.investai.api.module.relatorio.dto;

import com.investai.api.module.rendafixa.entity.Indexador;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record TituloRendaFixaRelatorio(
        String identificador,
        String nome,
        String categoria,
        Indexador indexador,
        BigDecimal taxa,
        boolean taxaSomadaAoIndexador,
        LocalDate vencimento,
        BigDecimal investimentoMinimo,
        String liquidez,
        String garantia,
        boolean isentoIr,
        List<RentabilidadePrazoDTO> rentabilidades
) {
}
