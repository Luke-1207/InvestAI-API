package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.dashboard.dto.SugestoesRendaVariavelResponseDTO;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcaoSugestaoService {

    private static final String MENSAGEM_PERFIL_INCOMPLETO =
            "Complete seu perfil para receber sugestões personalizadas.";

    private final AcaoRepository acaoRepository;
    private final AcaoPontuacaoService acaoPontuacaoService;
    private final PerfilInvestidorRepository perfilInvestidorRepository;

    public SugestoesRendaVariavelResponseDTO listarSugestoes(UUID usuarioId, List<TipoAtivo> filtroTipo) {
        PerfilInvestidor perfil = perfilInvestidorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil do investidor não encontrado"));

        if (!perfil.isPerfilPreenchido()) {
            return SugestoesRendaVariavelResponseDTO.builder()
                    .itens(List.of())
                    .mensagem(MENSAGEM_PERFIL_INCOMPLETO)
                    .build();
        }

        List<TipoAtivo> tiposAceitos = perfil.getTiposAceitos() == null ? List.of() :
                perfil.getTiposAceitos().stream().map(TipoAtivo::valueOf).toList();

        List<SugestaoAtivoItemDTO> itens = acaoRepository.findByAtivoTrue().stream()
                .filter(acao -> tiposAceitos.isEmpty() || tiposAceitos.contains(acao.getTipo()))
                .filter(acao -> filtroTipo == null || filtroTipo.isEmpty() || filtroTipo.contains(acao.getTipo()))
                .map(acao -> acaoPontuacaoService.pontuarAcao(acao, perfil))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SugestaoAtivoItemDTO::getScore).reversed())
                .toList();

        return SugestoesRendaVariavelResponseDTO.builder().itens(itens).build();
    }

    public SugestaoAtivoItemDTO obterSugestao(String codigo, UUID usuarioId) {
        PerfilInvestidor perfil = buscarPerfil(usuarioId);
        if (!perfil.isPerfilPreenchido()) {
            return null;
        }

        Acao acao = acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Ativo não cadastrado ou inativo: " + codigo));

        return acaoPontuacaoService.pontuarAcao(acao, perfil);
    }

    private PerfilInvestidor buscarPerfil(UUID usuarioId) {
        return perfilInvestidorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil do investidor não encontrado"));
    }
}