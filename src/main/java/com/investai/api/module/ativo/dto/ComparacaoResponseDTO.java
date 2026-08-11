package com.investai.api.module.ativo.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ComparacaoResponseDTO {
    private List<AcaoListagemResponseDTO> ativos;
}
