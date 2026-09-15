package com.investai.api.module.dashboard.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.ativo.service.AcaoPontuacaoService;
import com.investai.api.module.dashboard.dto.*;
import com.investai.api.module.perfil.entity.*;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.service.RendaFixaUnificadaService;
import com.investai.api.shared.scoring.PontuacaoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardSugestaoService {

    private static final int TOP_RENDA_VARIAVEL = 5;
    private static final int TOP_RENDA_FIXA = 3;
    private static final String MENSAGEM_PERFIL_INCOMPLETO = "Complete seu perfil para receber sugestões personalizadas.";

    private static final Set<Indexador> INDEXADORES_PREVISIVEIS = Set.of(Indexador.SELIC, Indexador.CDI);
    private static final int DIAS_LIMITE_CURTO_PRAZO = 365;
    private static final int DIAS_LIMITE_MEDIO_PRAZO = 1825;
    private static final List<HorizonteInvestimento> HORIZONTE_ORDEM = List.of(
            HorizonteInvestimento.CURTO_PRAZO, HorizonteInvestimento.MEDIO_PRAZO, HorizonteInvestimento.LONGO_PRAZO);

    private static final Map<String, String> TEMPLATES_JUSTIFICATIVA_FIXA = Map.of(
            "indexador_previsivel_conservador", "Indexador previsível, alinhado ao perfil conservador",
            "indexador_arrojado_prefixado", "Taxa prefixada é uma aposta compatível com o perfil arrojado",
            "indexador_protege_inflacao", "Indexação à inflação protege o poder de compra, alinhado a preservar capital",
            "vencimento_dentro_horizonte", "Vencimento compatível com o seu horizonte de investimento",
            "vencimento_muito_alem_horizonte", "Vencimento muito além do horizonte informado",
            "liquidez_diaria_curto_prazo", "Liquidez diária é importante pra quem pode precisar do dinheiro em breve",
            "isento_ir_renda_passiva", "Isenção de Imposto de Renda favorece o objetivo de renda passiva",
            "investimento_acessivel", "Investimento mínimo compatível com o valor disponível informado",
            "garantia_fgc_conservador", "Garantia do FGC traz segurança adicional, alinhada ao perfil conservador"
    );

    private final AcaoRepository acaoRepository;
    private final AcaoPontuacaoService acaoPontuacaoService;
    private final RendaFixaUnificadaService rendaFixaUnificadaService;
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
        Map<String, Integer> criterios = avaliarCriteriosRendaFixa(item, perfil);
        int score = PontuacaoUtil.normalizarScore(criterios);

        return SugestaoRendaFixaItemDTO.builder()
                .id(item.getId())
                .categoria(item.getCategoria())
                .nome(item.getNome())
                .taxa(item.getTaxa())
                .vencimento(item.getVencimento())
                .score(score)
                .compatibilidade(PontuacaoUtil.classificarCompatibilidade(score))
                .justificativa(PontuacaoUtil.gerarJustificativa(criterios, TEMPLATES_JUSTIFICATIVA_FIXA))
                .build();
    }

    private Map<String, Integer> avaliarCriteriosRendaFixa(RendaFixaListagemResponseDTO item, PerfilInvestidor perfil) {
        Map<String, Integer> criterios = new LinkedHashMap<>();
        PerfilRisco perfilRisco = PerfilRisco.valueOf(perfil.getPerfilRisco());
        ObjetivoFinanceiro objetivo = ObjetivoFinanceiro.valueOf(perfil.getObjetivo());
        HorizonteInvestimento horizonte = HorizonteInvestimento.valueOf(perfil.getHorizonte());
        Indexador indexador = Indexador.valueOf(item.getIndexador());

        if (perfilRisco == PerfilRisco.CONSERVADOR && INDEXADORES_PREVISIVEIS.contains(indexador)) {
            criterios.put("indexador_previsivel_conservador", 20);
        }
        if (perfilRisco == PerfilRisco.ARROJADO && indexador == Indexador.PREFIXADO) {
            criterios.put("indexador_arrojado_prefixado", 15);
        }
        if (objetivo == ObjetivoFinanceiro.PRESERVAR_CAPITAL && indexador == Indexador.IPCA) {
            criterios.put("indexador_protege_inflacao", 20);
        }

        int distancia = distanciaHorizonteVencimento(item.getVencimento(), horizonte);
        if (distancia == 0) {
            criterios.put("vencimento_dentro_horizonte", 20);
        } else if (distancia >= 2) {
            criterios.put("vencimento_muito_alem_horizonte", -15);
        }

        if (horizonte == HorizonteInvestimento.CURTO_PRAZO && "DIARIA".equals(item.getLiquidez())) {
            criterios.put("liquidez_diaria_curto_prazo", 15);
        }
        if (objetivo == ObjetivoFinanceiro.RENDA_PASSIVA && item.isIsentoIr()) {
            criterios.put("isento_ir_renda_passiva", 10);
        }
        if (perfil.getValorDisponivel() != null && item.getValorMinimo() != null
                && item.getValorMinimo().compareTo(perfil.getValorDisponivel()) <= 0) {
            criterios.put("investimento_acessivel", 10);
        }
        if (perfilRisco == PerfilRisco.CONSERVADOR && item.isGarantidoFgc()) {
            criterios.put("garantia_fgc_conservador", 10);
        }

        return criterios;
    }

    private int distanciaHorizonteVencimento(LocalDate vencimento, HorizonteInvestimento horizontePerfil) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), vencimento);
        HorizonteInvestimento horizonteTitulo = classificarHorizontePorDias(dias);
        return Math.abs(HORIZONTE_ORDEM.indexOf(horizonteTitulo) - HORIZONTE_ORDEM.indexOf(horizontePerfil));
    }

    private HorizonteInvestimento classificarHorizontePorDias(long dias) {
        if (dias <= DIAS_LIMITE_CURTO_PRAZO) return HorizonteInvestimento.CURTO_PRAZO;
        if (dias <= DIAS_LIMITE_MEDIO_PRAZO) return HorizonteInvestimento.MEDIO_PRAZO;
        return HorizonteInvestimento.LONGO_PRAZO;
    }
}