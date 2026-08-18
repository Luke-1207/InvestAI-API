package com.investai.api.module.perfil.dto;

import com.investai.api.module.perfil.entity.TipoPergunta;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuizPerguntaResponseDTO {
    private UUID id;
    private String texto;
    private TipoPergunta tipo;
    private boolean obrigatoria;
    private List<QuizOpcaoResponseDTO> opcoes;
}