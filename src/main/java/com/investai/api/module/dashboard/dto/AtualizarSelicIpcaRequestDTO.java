package com.investai.api.module.dashboard.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AtualizarSelicIpcaRequestDTO {

    @NotNull(message = "Selic atual é obrigatória")
    @Positive(message = "Selic atual deve ser positiva")
    private BigDecimal selicAtual;

    @NotNull(message = "IPCA acumulado é obrigatório")
    private BigDecimal ipcaAcumulado12m;
}