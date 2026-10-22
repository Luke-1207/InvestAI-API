package com.investai.api.module.integracao.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SaudeIntegracoesResponseDTO {
    private List<StatusIntegracaoDTO> integracoes;
    private LocalDateTime verificadoEm;
}
