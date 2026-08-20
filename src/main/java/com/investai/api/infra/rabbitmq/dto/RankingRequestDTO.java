package com.investai.api.infra.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RankingRequestDTO {
    private String correlationId;
    private ModuloIa modulo;
    private PerfilIaDTO perfil;
    private List<Map<String, Object>> ativos;
}