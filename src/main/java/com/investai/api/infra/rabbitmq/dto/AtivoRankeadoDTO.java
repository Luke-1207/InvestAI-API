package com.investai.api.infra.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AtivoRankeadoDTO {
    private String codigo;
    private Integer score;
    private Compatibilidade compatibilidade;
    private String justificativa;
}