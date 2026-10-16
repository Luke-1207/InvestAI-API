package com.investai.api.module.dashboard.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.ativo.service.AcaoPontuacaoService;
import com.investai.api.module.dashboard.dto.*;
import com.investai.api.module.perfil.entity.*;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.service.RendaFixaPontuacaoService;
import com.investai.api.module.rendafixa.service.RendaFixaUnificadaService;
import com.investai.api.shared.scoring.PontuacaoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardSugestaoService {

    private static final int TOP_RENDA_VARIAVEL = 5;
    private static final int TOP_RENDA_FIXA = 3;
    private static final String MENSAGEM_PERFIL_INCOMPLETO = "Complete seu perfil para receber sugestões personalizadas.";

    private final AcaoRepository acaoRepository;
    private final AcaoPontuacaoService acaoPontuacaoService;
    private final RendaFixaUnificadaService rendaFixaUnificadaService;
    private final RendaFixaPontuacaoService rendaFixaPontuacaoService;
    private final PerfilInvestidorRepository perfilInvestidorRepository;

    public SugestoesRendaVariavelResponseDTO sugerirRendaVariavel(UUID usuarioId) {
        PerfilInvestidor perfil = buscarPerfil(usuarioId);
        if (!perfil.isPerfilPreenchido()) {
            return SugestoesRendaVariavelResponseDTO.builder().itens(List.of()).mensagem(MENSAGEM_PERFIL_INCOMPLETO).build();
        }

        List<TipoAtivo> tiposAceitos = perfil.getTiposAceitos() == null ? List.of() :
                perfil.getTiposAceitos().stream().map(TipoAtivo::valueOf).toList();

        List<SugestaoAtivoItemDTO> sugestoes = acaoRepository.findByAtivoTrue().stream()
                .filter(acao -> tiposAceitos.isEmpty() || tiposAceitos.contains(acao.getTipo()))
                .map(acao -> acaoPontuacaoService.pontuarAcao(acao, perfil))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SugestaoAtivoItemDTO::getScore).reversed())
                .limit(TOP_RENDA_VARIAVEL)
                .toList();

        return SugestoesRendaVariavelResponseDTO.builder().itens(sugestoes).build();
    }

    public SugestoesRendaFixaResponseDTO sugerirRendaFixa(UUID usuarioId) {
        PerfilInvestidor perfil = buscarPerfil(usuarioId);
        if (!perfil.isPerfilPreenchido()) {
            return SugestoesRendaFixaResponseDTO.builder().itens(List.of()).mensagem(MENSAGEM_PERFIL_INCOMPLETO).build();
        }

        List<SugestaoRendaFixaItemDTO> sugestoes = rendaFixaUnificadaService.listar("livre", usuarioId).stream()
                .map(item -> pontuarRendaFixa(item, perfil))
                .sorted(Comparator.comparing(SugestaoRendaFixaItemDTO::getScore).reversed())
                .limit(TOP_RENDA_FIXA)
                .toList();

        return SugestoesRendaFixaResponseDTO.builder().itens(sugestoes).build();
    }

    private PerfilInvestidor buscarPerfil(UUID usuarioId) {
        return perfilInvestidorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil do investidor não encontrado"));
    }

    private SugestaoRendaFixaItemDTO pontuarRendaFixa(RendaFixaListagemResponseDTO item, PerfilInvestidor perfil) {
        Map<String, Integer> criterios = rendaFixaPontuacaoService.avaliarCriterios(item, perfil);
        int score = PontuacaoUtil.normalizarScore(criterios);

        return SugestaoRendaFixaItemDTO.builder()
                .id(item.getId())
                .categoria(item.getCategoria())
                .nome(item.getNome())
                .taxa(item.getTaxa())
                .vencimento(item.getVencimento())
                .score(score)
                .compatibilidade(PontuacaoUtil.classificarCompatibilidade(score))
                .justificativa(PontuacaoUtil.gerarJustificativa(criterios, RendaFixaPontuacaoService.TEMPLATES_JUSTIFICATIVA))
                .criterios(PontuacaoUtil.listarCriterios(criterios, RendaFixaPontuacaoService.DESCRICOES_CRITERIOS))
                .build();
    }
}
