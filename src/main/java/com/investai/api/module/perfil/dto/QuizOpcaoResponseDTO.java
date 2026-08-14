package com.investai.api.module.perfil.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class QuizOpcaoResponseDTO {
    private UUID id;
    private String texto;
    private String emoji;
}