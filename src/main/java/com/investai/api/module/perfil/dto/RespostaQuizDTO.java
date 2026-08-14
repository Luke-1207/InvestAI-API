package com.investai.api.module.perfil.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RespostaQuizDTO {

    @NotNull(message = "perguntaId é obrigatório")
    private UUID perguntaId;

    @NotEmpty(message = "Selecione ao menos uma opção")
    private List<UUID> opcaoIds;
}