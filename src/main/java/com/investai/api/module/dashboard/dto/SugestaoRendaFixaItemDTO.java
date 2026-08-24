package com.investai.api.module.dashboard.dto;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SugestaoRendaFixaItemDTO {
    private UUID id;
    private CategoriaRendaFixa categoria;
    private String nome;
    private BigDecimal taxa;
    private LocalDate vencimento;
    private Integer score;
    private Compatibilidade compatibilidade;
    private String justificativa;
}