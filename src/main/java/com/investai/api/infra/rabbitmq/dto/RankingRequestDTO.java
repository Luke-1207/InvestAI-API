package com.investai.api.infra.rabbitmq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingRequestDTO {
    private String correlationId;
    private ModuloIa modulo;
    private PerfilIaDTO perfil;
    private List<Map<String, Object>> ativos;
}