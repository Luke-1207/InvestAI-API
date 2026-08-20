package com.investai.api.infra.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumoResponseDTO {
    private String correlationId;
    private String resumo;
    private String erro;
}