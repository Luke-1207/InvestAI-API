package com.investai.api.module.perfil.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizSubmissaoResponseDTO {
    private ValorDescritoDTO perfilRisco;
    private ValorDescritoDTO objetivoFinanceiro;
    private ValorDescritoDTO horizonteInvestimento;
    private String resumoIA;
}