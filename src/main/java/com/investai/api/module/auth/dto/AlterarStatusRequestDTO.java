package com.investai.api.module.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlterarStatusRequestDTO {
    @NotNull(message = "Status é obrigatório")
    private Boolean ativo;
}
