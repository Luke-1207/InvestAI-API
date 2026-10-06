package com.investai.api.module.relatorio.dto;

import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DadosRelatorioAtivoVariavel(
        String nomeUsuario,
        LocalDateTime geradoEm,
        PerfilResponseDTO perfil,
        AcaoDetalheResponseDTO ativo,
        SugestaoAtivoItemDTO compatibilidade,
        String resumoIa
) {
}
