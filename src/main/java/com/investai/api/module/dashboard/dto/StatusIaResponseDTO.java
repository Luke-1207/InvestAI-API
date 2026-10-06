package com.investai.api.module.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusIaResponseDTO {
    private boolean disponivel;
    private Boolean rabbitmqConectado;
    private LocalDateTime verificadoEm;
}
