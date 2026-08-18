package com.investai.api.module.perfil.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmeterQuizRequestDTO {

    @NotEmpty(message = "É necessário informar ao menos uma resposta")
    @Valid
    private List<RespostaQuizDTO> respostas;
}