package com.investai.api.module.rendafixa.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SimulacaoRendimentoResponseDTO {
    private BigDecimal valorInvestido;
    private Integer prazoMeses;
    private Long prazoDias;
    private LocalDate dataResgate;
    private BigDecimal taxaAnualEstimada;
    private BigDecimal valorBruto;
    private BigDecimal rendimentoBruto;
    private BigDecimal aliquotaIR;
    private BigDecimal impostoRenda;
    private BigDecimal valorLiquido;
    private BigDecimal rendimentoLiquido;
    private BigDecimal taxaAnualPoupanca;
    private BigDecimal valorPoupanca;
    private BigDecimal rendimentoPoupanca;
    private BigDecimal diferencaParaPoupanca;
    private List<String> premissas;
    private List<String> avisos;
    private LocalDateTime indicadoresAtualizadosEm;
}
