package com.investai.api.module.perfil.dto;

import com.investai.api.module.perfil.entity.PreferenciaSetor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetorPreferidoRequestDTO {

    @NotBlank(message = "setor é obrigatório")
    private String setor;

    @NotNull(message = "preferencia é obrigatória")
    private PreferenciaSetor preferencia;
}