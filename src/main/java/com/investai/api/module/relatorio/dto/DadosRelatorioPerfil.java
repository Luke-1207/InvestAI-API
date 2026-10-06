package com.investai.api.module.relatorio.dto;

import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DadosRelatorioPerfil(
        String nomeUsuario,
        String email,
        String telefone,
        LocalDateTime cadastradoEm,
        LocalDateTime geradoEm,
        PerfilResponseDTO perfil
) {
}
