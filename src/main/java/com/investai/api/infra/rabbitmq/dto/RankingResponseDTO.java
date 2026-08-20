package com.investai.api.infra.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RankingResponseDTO {
    private String correlationId;
    private List<AtivoRankeadoDTO> ativos;
}