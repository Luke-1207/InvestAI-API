package com.investai.api.module.rendafixa.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TituloTesouroDetalheResponseDTO {
    private String codigo;
    private String nome;
    private ValorDescritoDTO tipo;
    private BigDecimal taxaAnual;
    private BigDecimal precoMinimo;
    private LocalDate vencimento;
    private boolean pagaJurosSemestrais;
    private String liquidez;
    private String resumoIA;
}