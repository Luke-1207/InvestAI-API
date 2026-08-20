package com.investai.api.module.rendafixa.dto;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class RendaFixaListagemResponseDTO {
    private UUID id;
    private CategoriaRendaFixa categoria;
    private String nome;
    private String indexador;
    private BigDecimal taxa;
    private LocalDate vencimento;
    private BigDecimal valorMinimo;
    private String liquidez;
    private boolean isentoIr;
    private boolean garantidoFgc;
    private Integer score;
    private Compatibilidade compatibilidade;
    private String justificativa;
}