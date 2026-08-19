package com.investai.api.infra.tesourodireto.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class TituloTesouroExternoDTO {
    private String codigo;
    private String nome;
    private String tipo;
    private BigDecimal taxaAnual;
    private BigDecimal precoMinimo;
    private LocalDate vencimento;
    private boolean pagaJurosSemestrais;
}