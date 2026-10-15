package com.investai.api.module.ativo.service;

import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.dashboard.dto.VolatilidadeAtivo;
import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PerfilRisco;
import com.investai.api.module.perfil.entity.PreferenciaSetor;
import com.investai.api.shared.scoring.PontuacaoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AcaoPontuacaoService {

    private static final BigDecimal VOLATILIDADE_LIMIAR_BAIXA = BigDecimal.valueOf(1);
    private static final BigDecimal VOLATILIDADE_LIMIAR_MEDIA = BigDecimal.valueOf(3);

    static final int VOLATILIDADE_IDEAL = 35;
    static final int VOLATILIDADE_PROXIMA = 15;
    static final int VOLATILIDADE_OPOSTA = -10;
    static final int DY_ALINHADO = 25;
    static final int DY_PARCIAL = 12;
    static final int DY_FRACO = 5;
    static final int HORIZONTE_LONGO = 15;
    static final int HORIZONTE_MEDIO = 8;
    static final int PRECO_ACESSIVEL = 10;
    static final int SETOR_PREFERIDO = 15;
    static final int SETOR_NEUTRO = 8;
    static final int SETOR_EVITADO = -20;

    private static final BigDecimal DY_ALTO = BigDecimal.valueOf(6);
    private static final BigDecimal DY_BAIXO = BigDecimal.valueOf(3);

    private static final Map<String, String> TEMPLATES_JUSTIFICATIVA_ACAO = Map.ofEntries(
            Map.entry("volatilidade_ideal", "Oscilação de preço compatível com o seu perfil de risco"),
            Map.entry("volatilidade_proxima", "Oscilação de preço aceitável para o seu perfil de risco"),
            Map.entry("volatilidade_alta_demais", "Oscila mais do que o seu perfil de risco costuma tolerar"),
            Map.entry("volatilidade_baixa_demais", "Oscila pouco, com potencial de retorno abaixo do que o seu perfil busca"),
            Map.entry("dy_renda_passiva", "Dividend yield alto, alinhado ao seu objetivo de renda passiva"),
            Map.entry("dy_renda_passiva_parcial", "Paga dividendos moderados, o que ajuda no objetivo de renda passiva"),
            Map.entry("dy_crescimento", "Baixo DY sugere reinvestimento do lucro, alinhado ao crescimento de patrimônio"),
            Map.entry("dy_crescimento_parcial", "Combina dividendos moderados com espaço para crescer"),
            Map.entry("dy_preservar", "Distribui parte do lucro aos acionistas, característica de empresas mais maduras"),
            Map.entry("horizonte_longo", "Renda variável combina com o seu horizonte de longo prazo"),
            Map.entry("horizonte_medio", "Seu horizonte de médio prazo comporta uma parte em renda variável"),
            Map.entry("preco_acessivel", "Preço compatível com o valor que você tem disponível"),
            Map.entry("setor_preferido", "Setor está entre os que você prefere"),
            Map.entry("setor_evitado", "Setor está entre os que você prefere evitar")
    );

    private final CotacaoService cotacaoService;

    public SugestaoAtivoItemDTO pontuarAcao(Acao acao, PerfilInvestidor perfil) {
        CotacaoResponseDTO cotacao;
        try {
            cotacao = cotacaoService.obterCotacao(acao.getCodigo());
        } catch (Exception e) {
            return null;
        }

        Map<String, Integer> criterios = avaliarCriteriosAcao(acao, cotacao, perfil);
        int score = PontuacaoUtil.normalizarScore(criterios);

        return SugestaoAtivoItemDTO.builder()
                .codigo(acao.getCodigo())
                .nome(acao.getNome())
                .tipo(acao.getTipo())
                .setor(acao.getSetor())
                .preco(cotacao.getPreco())
                .variacaoDia(cotacao.getVariacaoPercentual())
                .dy(cotacao.getDividendYield())
                .score(score)
                .compatibilidade(PontuacaoUtil.classificarCompatibilidade(score))
                .justificativa(PontuacaoUtil.gerarJustificativa(criterios, TEMPLATES_JUSTIFICATIVA_ACAO))
                .build();
    }

    private Map<String, Integer> avaliarCriteriosAcao(Acao acao, CotacaoResponseDTO cotacao, PerfilInvestidor perfil) {
        Map<String, Integer> criterios = new LinkedHashMap<>();
        PerfilRisco perfilRisco = PerfilRisco.valueOf(perfil.getPerfilRisco());
        ObjetivoFinanceiro objetivo = ObjetivoFinanceiro.valueOf(perfil.getObjetivo());
        HorizonteInvestimento horizonte = HorizonteInvestimento.valueOf(perfil.getHorizonte());
        BigDecimal dy = cotacao.getDividendYield() == null ? BigDecimal.ZERO : cotacao.getDividendYield();

        avaliarVolatilidade(criterios, perfilRisco, classificarVolatilidade(cotacao.getVariacaoPercentual()));
        avaliarDividendos(criterios, objetivo, dy);

        if (horizonte == HorizonteInvestimento.LONGO_PRAZO) criterios.put("horizonte_longo", HORIZONTE_LONGO);
        if (horizonte == HorizonteInvestimento.MEDIO_PRAZO) criterios.put("horizonte_medio", HORIZONTE_MEDIO);

        if (perfil.getValorDisponivel() != null && cotacao.getPreco() != null
                && cotacao.getPreco().compareTo(perfil.getValorDisponivel()) <= 0) {
            criterios.put("preco_acessivel", PRECO_ACESSIVEL);
        }

        if (setorNaListaComPreferencia(acao.getSetor(), perfil, PreferenciaSetor.PREFERIR)) {
            criterios.put("setor_preferido", SETOR_PREFERIDO);
        } else if (setorNaListaComPreferencia(acao.getSetor(), perfil, PreferenciaSetor.EVITAR)) {
            criterios.put("setor_evitado", SETOR_EVITADO);
        } else {
            criterios.put("setor_neutro", SETOR_NEUTRO);
        }

        return criterios;
    }

    private void avaliarVolatilidade(Map<String, Integer> criterios, PerfilRisco risco, VolatilidadeAtivo volatilidade) {
        int distancia = Math.abs(risco.ordinal() - volatilidade.ordinal());
        if (distancia == 0) {
            criterios.put("volatilidade_ideal", VOLATILIDADE_IDEAL);
        } else if (distancia == 1) {
            criterios.put("volatilidade_proxima", VOLATILIDADE_PROXIMA);
        } else if (volatilidade == VolatilidadeAtivo.ALTA) {
            criterios.put("volatilidade_alta_demais", VOLATILIDADE_OPOSTA);
        } else {
            criterios.put("volatilidade_baixa_demais", VOLATILIDADE_OPOSTA);
        }
    }

    private void avaliarDividendos(Map<String, Integer> criterios, ObjetivoFinanceiro objetivo, BigDecimal dy) {
        boolean dyAlto = dy.compareTo(DY_ALTO) >= 0;
        boolean dyBaixo = dy.compareTo(DY_BAIXO) < 0;
        switch (objetivo) {
            case RENDA_PASSIVA -> {
                if (dyAlto) criterios.put("dy_renda_passiva", DY_ALINHADO);
                else if (!dyBaixo) criterios.put("dy_renda_passiva_parcial", DY_PARCIAL);
            }
            case CRESCIMENTO_PATRIMONIO -> {
                if (dyBaixo) criterios.put("dy_crescimento", DY_ALINHADO);
                else if (!dyAlto) criterios.put("dy_crescimento_parcial", DY_PARCIAL);
                else criterios.put("dy_crescimento_fraco", DY_FRACO);
            }
            case PRESERVAR_CAPITAL -> {
                if (!dyBaixo) criterios.put("dy_preservar", DY_ALINHADO);
                else criterios.put("dy_preservar_fraco", DY_FRACO);
            }
        }
    }

    private VolatilidadeAtivo classificarVolatilidade(BigDecimal variacaoDia) {
        if (variacaoDia == null) return VolatilidadeAtivo.MEDIA;
        BigDecimal absoluta = variacaoDia.abs();
        if (absoluta.compareTo(VOLATILIDADE_LIMIAR_BAIXA) <= 0) return VolatilidadeAtivo.BAIXA;
        if (absoluta.compareTo(VOLATILIDADE_LIMIAR_MEDIA) <= 0) return VolatilidadeAtivo.MEDIA;
        return VolatilidadeAtivo.ALTA;
    }

    private boolean setorNaListaComPreferencia(String setorAtivo, PerfilInvestidor perfil, PreferenciaSetor preferenciaAlvo) {
        if (setorAtivo == null || perfil.getSetoresPreferidos() == null) return false;
        return perfil.getSetoresPreferidos().stream()
                .anyMatch(s -> setorAtivo.equalsIgnoreCase(s.getSetor()) && s.getPreferencia() == preferenciaAlvo);
    }
}