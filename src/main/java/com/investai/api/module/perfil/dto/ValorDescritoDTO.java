package com.investai.api.module.perfil.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValorDescritoDTO {
    private String valor;
    private String descricao;
}