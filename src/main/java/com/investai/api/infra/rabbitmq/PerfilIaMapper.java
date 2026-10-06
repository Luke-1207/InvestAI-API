package com.investai.api.infra.rabbitmq;

import com.investai.api.infra.rabbitmq.dto.PerfilIaDTO;
import com.investai.api.infra.rabbitmq.dto.SetorPreferidoIaDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PerfilRisco;

import java.util.List;

public final class PerfilIaMapper {

    private PerfilIaMapper() {
    }

    public static PerfilIaDTO dePerfil(PerfilInvestidor perfil) {
        return PerfilIaDTO.builder()
                .perfilRisco(perfil.getPerfilRisco() != null ? PerfilRisco.valueOf(perfil.getPerfilRisco()) : null)
                .horizonte(perfil.getHorizonte() != null ? HorizonteInvestimento.valueOf(perfil.getHorizonte()) : null)
                .objetivo(perfil.getObjetivo() != null ? ObjetivoFinanceiro.valueOf(perfil.getObjetivo()) : null)
                .valorDisponivel(perfil.getValorDisponivel())
                .tiposAceitos(perfil.getTiposAceitos() == null ? List.of() :
                        perfil.getTiposAceitos().stream().map(TipoAtivo::valueOf).toList())
                .setoresPreferidos(perfil.getSetoresPreferidos() == null ? List.of() :
                        perfil.getSetoresPreferidos().stream()
                                .map(s -> SetorPreferidoIaDTO.builder()
                                        .setor(s.getSetor())
                                        .preferencia(s.getPreferencia())
                                        .build())
                                .toList())
                .build();
    }
}
