package com.investai.api.infra.rabbitmq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingResponseDTO {
    private String correlationId;
    private List<AtivoRankeadoDTO> ativos;
}