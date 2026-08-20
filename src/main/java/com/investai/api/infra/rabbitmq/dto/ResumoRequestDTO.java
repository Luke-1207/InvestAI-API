package com.investai.api.infra.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ResumoRequestDTO {
    private String correlationId;
    private ModuloIa modulo;
    private PerfilIaDTO perfil;
    private Map<String, Object> ativo;
}