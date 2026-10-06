package com.investai.api.module.relatorio.dto;

import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DadosRelatorioAtivoFixo(
        String nomeUsuario,
        LocalDateTime geradoEm,
        PerfilResponseDTO perfil,
        TituloRendaFixaRelatorio titulo
) {
}
