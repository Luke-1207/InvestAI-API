package com.investai.api.module.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SugestoesRendaVariavelResponseDTO {
    private List<SugestaoAtivoItemDTO> itens;
    private String mensagem;
}