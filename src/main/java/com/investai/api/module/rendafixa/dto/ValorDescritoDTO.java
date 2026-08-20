package com.investai.api.module.rendafixa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValorDescritoDTO {
    private String valor;
    private String descricao;
}