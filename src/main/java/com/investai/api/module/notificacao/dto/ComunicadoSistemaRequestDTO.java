package com.investai.api.module.notificacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComunicadoSistemaRequestDTO {

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
    private String titulo;

    @NotBlank(message = "A mensagem é obrigatória")
    @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres")
    private String mensagem;
}
