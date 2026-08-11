package com.investai.api.module.ativo.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HistoricoPrecoResponseDTO {
    private String codigo;
    private String periodo;
    private List<PontoHistoricoDTO> pontos;
}