package com.investai.api.infra.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RankingRequestDTO {
    private String correlationId;
    private ModuloIa modulo;
    private PerfilIaDTO perfil;
    private List<AtivoRankingDTO> ativos;
}