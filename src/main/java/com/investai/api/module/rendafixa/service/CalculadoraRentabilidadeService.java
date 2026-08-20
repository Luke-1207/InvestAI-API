package com.investai.api.module.rendafixa.service;

import com.investai.api.module.rendafixa.dto.RentabilidadeEstimadaDTO;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calcula a rentabilidade líquida estimada de um título privado, aplicando
 * o desconto de IR regressivo sobre o taxaPercentual bruto.
 * <p>
 * Importante: como ainda não temos indicador de mercado ao vivo (Selic/CDI —
 * isso é o INVAI-50, fora dessa sprint), o cálculo aplica o desconto de IR
 * proporcionalmente sobre o taxaPercentual guardado (seja ele "% do CDI",
 * "% a.a." pro prefixado, ou o spread do IPCA+), em vez de resolver pro
 * valor absoluto em R$/ano. É matematicamente válido — IR é um percentual
 * reto sobre o rendimento — mas o número fica na mesma unidade do
 * taxaPercentual original, não necessariamente "% a.a." puro.
 */
@Service
public class CalculadoraRentabilidadeService {

    public RentabilidadeEstimadaDTO calcular(TituloPrivado titulo) {
        BigDecimal taxaBruta = titulo.getTaxaPercentual();
        BigDecimal aliquotaIR = titulo.isIsentoIr() ? BigDecimal.ZERO : calcularAliquotaIR(titulo.getVencimento());

        BigDecimal fatorLiquido = BigDecimal.ONE.subtract(
                aliquotaIR.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        BigDecimal taxaLiquida = taxaBruta.multiply(fatorLiquido).setScale(2, RoundingMode.HALF_UP);

        return RentabilidadeEstimadaDTO.builder()
                .taxaBrutaAnual(taxaBruta)
                .aliquotaIR(aliquotaIR)
                .taxaLiquidaAnual(taxaLiquida)
                .build();
    }

    private BigDecimal calcularAliquotaIR(LocalDate vencimento) {
        long diasParaVencimento = ChronoUnit.DAYS.between(LocalDate.now(), vencimento);

        if (diasParaVencimento <= 180) return BigDecimal.valueOf(22.5);
        if (diasParaVencimento <= 360) return BigDecimal.valueOf(20.0);
        if (diasParaVencimento <= 720) return BigDecimal.valueOf(17.5);
        return BigDecimal.valueOf(15.0);
    }
}