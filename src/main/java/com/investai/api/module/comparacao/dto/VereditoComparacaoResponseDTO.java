package com.investai.api.module.comparacao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VereditoComparacaoResponseDTO {
    private String veredito;
    private boolean simplificado;
}
