package com.investai.api.module.perfil.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizResponseDTO {
    private List<QuizPerguntaResponseDTO> perguntas;
}