package com.investai.api.infra.rabbitmq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparacaoIaResponseDTO {
    private String correlationId;
    private String veredito;
    private String erro;
}
