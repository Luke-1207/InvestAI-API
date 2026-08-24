package com.investai.api.infra.ia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IaHealthResponseDTO {
    private String status;
    private Boolean rabbitmq;
}