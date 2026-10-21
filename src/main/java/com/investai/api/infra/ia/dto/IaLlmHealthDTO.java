package com.investai.api.infra.ia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IaLlmHealthDTO {
    private boolean disponivel;
    private String provider;
    private String modelo;
    private String detalhe;
}
