package com.investai.api.infra.rabbitmq.dto;

import com.investai.api.shared.scoring.CriterioScoreDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtivoRankeadoDTO {
    private String codigo;
    private Integer score;
    private Compatibilidade compatibilidade;
    private String justificativa;
    private List<CriterioScoreDTO> criterios;
}