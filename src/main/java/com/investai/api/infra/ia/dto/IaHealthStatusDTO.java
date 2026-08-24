package com.investai.api.infra.ia.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IaHealthStatusDTO {
    private boolean disponivel;
    private Boolean rabbitmqConectado;
}