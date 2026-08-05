package com.investai.api.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExcluirContaRequestDTO {
    @NotBlank(message = "Senha é obrigatória para confirmar a exclusão")
    private String senha;
}
