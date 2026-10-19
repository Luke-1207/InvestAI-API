package com.investai.api.module.dashboard.dto;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import com.investai.api.shared.scoring.CriterioScoreDTO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SugestaoRendaFixaItemDTO {
    private UUID id;
    private String codigo;
    private CategoriaRendaFixa categoria;
    private String nome;
    private BigDecimal taxa;
    private LocalDate vencimento;
    private Integer score;
    private Compatibilidade compatibilidade;
    private String justificativa;
    private List<CriterioScoreDTO> criterios;
}