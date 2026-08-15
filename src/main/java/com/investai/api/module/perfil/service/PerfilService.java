package com.investai.api.module.perfil.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.dto.*;
import com.investai.api.module.perfil.entity.*;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilInvestidorRepository perfilInvestidorRepository;
    private final ResumoPerfilService resumoPerfilService;

    @Transactional(readOnly = true)
    public PerfilResponseDTO obterPerfil(UUID usuarioId) {
        PerfilInvestidor perfil = buscarPerfil(usuarioId);
        return montarPerfilResponse(perfil);
    }

    @Transactional
    public PerfilResponseDTO editarPerfil(UUID usuarioId, EditarPerfilRequestDTO dto) {
        PerfilInvestidor perfil = buscarPerfil(usuarioId);

        perfil.setPerfilRisco(dto.getPerfilRisco().name());
        perfil.setHorizonte(dto.getHorizonteInvestimento().name());
        perfil.setObjetivo(dto.getObjetivoFinanceiro().name());
        perfil.setValorDisponivel(dto.getValorDisponivel());
        perfil.setTiposAceitos(dto.getTiposAceitos().stream().map(Enum::name).toList());
        perfil.setSetoresPreferidos(dto.getSetoresPreferidos().stream()
                .map(s -> SetorPreferido.builder()
                        .setor(s.getSetor())
                        .preferencia(s.getPreferencia())
                        .build())
                .toList());
        perfil.setPerfilPreenchido(true);

        perfilInvestidorRepository.save(perfil);

        return montarPerfilResponse(perfil);
    }

    private PerfilInvestidor buscarPerfil(UUID usuarioId) {
        return perfilInvestidorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil do investidor não encontrado"));
    }

    private PerfilResponseDTO montarPerfilResponse(PerfilInvestidor perfil) {
        return PerfilResponseDTO.builder()
                .perfilRisco(construirPerfilRiscoDTO(perfil.getPerfilRisco()))
                .objetivoFinanceiro(construirObjetivoDTO(perfil.getObjetivo()))
                .horizonteInvestimento(construirHorizonteDTO(perfil.getHorizonte()))
                .valorDisponivel(perfil.getValorDisponivel())
                .tiposAceitos(perfil.getTiposAceitos() == null ? List.of() :
                        perfil.getTiposAceitos().stream().map(TipoAtivo::valueOf).toList())
                .setoresPreferidos(perfil.getSetoresPreferidos() == null ? List.of() : perfil.getSetoresPreferidos())
                .perfilPreenchido(perfil.isPerfilPreenchido())
                .resumoIA(resumoPerfilService.gerarResumoIA(perfil))
                .atualizadoEm(perfil.getAtualizadoEm())
                .build();
    }

    private ValorDescritoDTO construirPerfilRiscoDTO(String valor) {
        if (valor == null) return null;
        PerfilRisco perfilRisco = PerfilRisco.valueOf(valor);
        return ValorDescritoDTO.builder()
                .valor(perfilRisco.name())
                .descricao(DescricoesPerfil.PERFIL_RISCO.get(perfilRisco))
                .build();
    }

    private ValorDescritoDTO construirObjetivoDTO(String valor) {
        if (valor == null) return null;
        ObjetivoFinanceiro objetivo = ObjetivoFinanceiro.valueOf(valor);
        return ValorDescritoDTO.builder()
                .valor(objetivo.name())
                .descricao(DescricoesPerfil.OBJETIVO_FINANCEIRO.get(objetivo))
                .build();
    }

    private ValorDescritoDTO construirHorizonteDTO(String valor) {
        if (valor == null) return null;
        HorizonteInvestimento horizonte = HorizonteInvestimento.valueOf(valor);
        return ValorDescritoDTO.builder()
                .valor(horizonte.name())
                .descricao(DescricoesPerfil.HORIZONTE_INVESTIMENTO.get(horizonte))
                .build();
    }
}