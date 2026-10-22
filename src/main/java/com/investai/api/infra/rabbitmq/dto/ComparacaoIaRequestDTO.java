package com.investai.api.infra.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ComparacaoIaRequestDTO {
    private String correlationId;
    private PerfilIaDTO perfil;
    private Map<String, Object> ativoA;
    private Map<String, Object> ativoB;
}
