package com.investai.api.module.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SugestoesRendaFixaResponseDTO {
    private List<SugestaoRendaFixaItemDTO> itens;
    private String mensagem;
}