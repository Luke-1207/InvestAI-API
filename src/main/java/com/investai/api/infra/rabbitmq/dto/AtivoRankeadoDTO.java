package com.investai.api.infra.rabbitmq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtivoRankeadoDTO {
    private String codigo;
    private Integer score;
    private Compatibilidade compatibilidade;
    private String justificativa;
}