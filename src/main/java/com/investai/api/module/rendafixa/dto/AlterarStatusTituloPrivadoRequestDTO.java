package com.investai.api.module.rendafixa.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlterarStatusTituloPrivadoRequestDTO {
    @NotNull(message = "Status é obrigatório")
    private Boolean ativo;
}