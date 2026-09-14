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

    private static final Map<String, String> TEMPLATES_JUSTIFICATIVA_ACAO = Map.of(
            "volatilidade_favoravel", "Volatilidade compatível com o seu perfil de risco",
            "volatilidade_desfavoravel", "Volatilidade além do que seu perfil de risco costuma tolerar",
            "dy_renda_passiva", "Dividend yield alto, alinhado ao seu objetivo de renda passiva",
            "dy_crescimento", "Baixo DY sugere reinvestimento de lucro, alinhado ao crescimento de patrimônio",
            "preco_acessivel", "Preço compatível com o valor que você tem disponível",
            "setor_preferido", "Setor está entre os que você prefere",
            "setor_evitado", "Setor está entre os que você prefere evitar",
            "horizonte_crescimento", "Perfil de crescimento alinhado ao seu horizonte de longo prazo"
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

        VolatilidadeAtivo volatilidade = classificarVolatilidade(cotacao.getVariacaoPercentual());
        if (combinacaoFavoravelVolatilidade(perfilRisco, volatilidade)) criterios.put("volatilidade_favoravel", 20);
        if (combinacaoDesfavoravelVolatilidade(perfilRisco, volatilidade)) criterios.put("volatilidade_desfavoravel", -30);

        BigDecimal dy = cotacao.getDividendYield();
        if (objetivo == ObjetivoFinanceiro.RENDA_PASSIVA && dy != null && dy.compareTo(BigDecimal.valueOf(6)) >= 0) {
            criterios.put("dy_renda_passiva", 15);
        }
        if (objetivo == ObjetivoFinanceiro.CRESCIMENTO_PATRIMONIO && dy != null && dy.compareTo(BigDecimal.valueOf(3)) < 0) {
            criterios.put("dy_crescimento", 10);
        }

        if (perfil.getValorDisponivel() != null && cotacao.getPreco() != null
                && cotacao.getPreco().compareTo(perfil.getValorDisponivel()) <= 0) {
            criterios.put("preco_acessivel", 10);
        }

        if (setorNaListaComPreferencia(acao.getSetor(), perfil, PreferenciaSetor.PREFERIR)) criterios.put("setor_preferido", 15);
        if (setorNaListaComPreferencia(acao.getSetor(), perfil, PreferenciaSetor.EVITAR)) criterios.put("setor_evitado", -20);

        if (horizonte == HorizonteInvestimento.LONGO_PRAZO && dy != null && dy.compareTo(BigDecimal.valueOf(3)) < 0) {
            criterios.put("horizonte_crescimento", 10);
        }

        return criterios;
    }

    private VolatilidadeAtivo classificarVolatilidade(BigDecimal variacaoDia) {
        if (variacaoDia == null) return VolatilidadeAtivo.MEDIA;
        BigDecimal absoluta = variacaoDia.abs();
        if (absoluta.compareTo(VOLATILIDADE_LIMIAR_BAIXA) <= 0) return VolatilidadeAtivo.BAIXA;
        if (absoluta.compareTo(VOLATILIDADE_LIMIAR_MEDIA) <= 0) return VolatilidadeAtivo.MEDIA;
        return VolatilidadeAtivo.ALTA;
    }

    private boolean combinacaoFavoravelVolatilidade(PerfilRisco risco, VolatilidadeAtivo volatilidade) {
        return (risco == PerfilRisco.CONSERVADOR && volatilidade == VolatilidadeAtivo.BAIXA)
                || (risco == PerfilRisco.MODERADO && volatilidade == VolatilidadeAtivo.MEDIA)
                || (risco == PerfilRisco.ARROJADO && volatilidade == VolatilidadeAtivo.ALTA);
    }

    private boolean combinacaoDesfavoravelVolatilidade(PerfilRisco risco, VolatilidadeAtivo volatilidade) {
        return (risco == PerfilRisco.CONSERVADOR && volatilidade == VolatilidadeAtivo.ALTA)
                || (risco == PerfilRisco.ARROJADO && volatilidade == VolatilidadeAtivo.BAIXA);
    }

    private boolean setorNaListaComPreferencia(String setorAtivo, PerfilInvestidor perfil, PreferenciaSetor preferenciaAlvo) {
        if (setorAtivo == null || perfil.getSetoresPreferidos() == null) return false;
        return perfil.getSetoresPreferidos().stream()
                .anyMatch(s -> setorAtivo.equalsIgnoreCase(s.getSetor()) && s.getPreferencia() == preferenciaAlvo);
    }
}