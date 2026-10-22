package com.investai.api.module.rendafixa.service;

import com.investai.api.module.rendafixa.dto.RentabilidadeEstimadaDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class CalculadoraRentabilidadeService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    public RentabilidadeEstimadaDTO calcular(TituloPrivado titulo) {
        return calcularParaPrazo(titulo.getTaxaPercentual(), titulo.isIsentoIr(), diasAte(titulo.getVencimento()),
                taxaSomadaAoIndexador(titulo));
    }

    public RentabilidadeEstimadaDTO calcular(TituloTesouro titulo) {
        return calcularParaPrazo(titulo.getTaxaAnual(), false, diasAte(titulo.getVencimento()),
                taxaSomadaAoIndexador(titulo));
    }

    public static boolean taxaSomadaAoIndexador(TituloPrivado titulo) {
        return titulo.getIndexador() == Indexador.IPCA;
    }

    public static boolean taxaSomadaAoIndexador(TituloTesouro titulo) {
        return titulo.getTipo() != TipoTesouro.PREFIXADO;
    }

    public RentabilidadeEstimadaDTO calcularParaPrazo(BigDecimal taxaBruta, boolean isentoIr, long prazoEmDias) {
        return calcularParaPrazo(taxaBruta, isentoIr, prazoEmDias, false);
    }

    public RentabilidadeEstimadaDTO calcularParaPrazo(BigDecimal taxaBruta, boolean isentoIr, long prazoEmDias,
                                                      boolean taxaSomadaAoIndexador) {
        BigDecimal aliquotaIR = isentoIr ? BigDecimal.ZERO : calcularAliquotaIR(prazoEmDias);
        BigDecimal percentualLiquido = CEM.subtract(aliquotaIR);
        boolean liquidaNaMesmaUnidade = isentoIr || !taxaSomadaAoIndexador;

        BigDecimal taxaLiquida = liquidaNaMesmaUnidade
                ? taxaBruta.multiply(percentualLiquido).divide(CEM, 2, RoundingMode.HALF_UP)
                : null;

        return RentabilidadeEstimadaDTO.builder()
                .taxaBrutaAnual(taxaBruta)
                .aliquotaIR(aliquotaIR)
                .taxaLiquidaAnual(taxaLiquida)
                .taxaSomadaAoIndexador(taxaSomadaAoIndexador)
                .percentualRendimentoLiquido(percentualLiquido)
                .build();
    }

    public BigDecimal calcularAliquotaIR(long diasParaVencimento) {
        if (diasParaVencimento <= 180) return BigDecimal.valueOf(22.5);
        if (diasParaVencimento <= 360) return BigDecimal.valueOf(20.0);
        if (diasParaVencimento <= 720) return BigDecimal.valueOf(17.5);
        return BigDecimal.valueOf(15.0);
    }

    private long diasAte(LocalDate vencimento) {
        return ChronoUnit.DAYS.between(LocalDate.now(), vencimento);
    }
}
