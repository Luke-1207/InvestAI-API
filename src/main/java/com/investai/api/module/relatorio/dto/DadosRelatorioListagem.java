package com.investai.api.module.relatorio.dto;

import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
public record DadosRelatorioListagem(
        String nomeUsuario,
        LocalDateTime geradoEm,
        PerfilResponseDTO perfil,
        ModuloRelatorio modulo,
        Map<String, String> filtros,
        List<ItemListagemRelatorio> itens,
        int totalNaListagem,
        List<String> ativosNaoEncontrados
) {
}
