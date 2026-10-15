package com.investai.api.module.rendafixa.service;

import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PerfilRisco;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RendaFixaPontuacaoService {

    static final int INDEXADOR_IDEAL = 30;
    static final int VENCIMENTO_NO_HORIZONTE = 25;
    static final int VENCIMENTO_PROXIMO = 10;
    static final int VENCIMENTO_MUITO_ALEM = -10;
    static final int LIQUIDEZ_ADEQUADA = 15;
    static final int LIQUIDEZ_PARCIAL = 8;
    static final int OBJETIVO_ALINHADO = 15;
    static final int OBJETIVO_POS_FIXADO_PRESERVAR = 10;
    static final int OBJETIVO_PARCIAL = 8;
    static final int OBJETIVO_FRACO = 5;
    static final int INVESTIMENTO_ACESSIVEL = 10;
    static final int GARANTIA = 5;

    private static final Map<PerfilRisco, Map<Indexador, Integer>> PESO_INDEXADOR = Map.of(
            PerfilRisco.CONSERVADOR, Map.of(Indexador.SELIC, 30, Indexador.CDI, 30, Indexador.IPCA, 15, Indexador.PREFIXADO, 5),
            PerfilRisco.MODERADO, Map.of(Indexador.SELIC, 20, Indexador.CDI, 20, Indexador.IPCA, 30, Indexador.PREFIXADO, 20),
            PerfilRisco.ARROJADO, Map.of(Indexador.SELIC, 10, Indexador.CDI, 10, Indexador.IPCA, 25, Indexador.PREFIXADO, 30)
    );

    private static final int DIAS_LIMITE_CURTO_PRAZO = 365;
    private static final int DIAS_LIMITE_MEDIO_PRAZO = 1825;
    private static final List<HorizonteInvestimento> HORIZONTE_ORDEM = List.of(
            HorizonteInvestimento.CURTO_PRAZO, HorizonteInvestimento.MEDIO_PRAZO, HorizonteInvestimento.LONGO_PRAZO);

    public static final Map<String, String> TEMPLATES_JUSTIFICATIVA = Map.ofEntries(
            Map.entry("indexador_ideal", "Indexador mais indicado para o seu perfil de risco"),
            Map.entry("indexador_adequado", "Indexador combina bem com o seu perfil de risco"),
            Map.entry("vencimento_no_horizonte", "Vencimento compatível com o seu horizonte de investimento"),
            Map.entry("vencimento_proximo_horizonte", "Vencimento um pouco diferente do seu horizonte, mas ainda razoável"),
            Map.entry("vencimento_muito_alem_horizonte", "Vencimento muito distante do horizonte que você informou"),
            Map.entry("liquidez_diaria", "Liquidez diária, você pode resgatar quando precisar"),
            Map.entry("liquidez_no_vencimento_longo", "Resgate só no vencimento, sem problema para o seu horizonte de longo prazo"),
            Map.entry("isento_ir_renda_passiva", "Isenção de Imposto de Renda favorece o objetivo de renda passiva"),
            Map.entry("indexador_protege_inflacao", "Indexação ao IPCA protege o poder de compra, alinhado a preservar capital"),
            Map.entry("pos_fixado_preservar", "Rendimento que acompanha os juros, estável para preservar capital"),
            Map.entry("taxa_crescimento", "Taxa prefixada ou ligada à inflação favorece o crescimento no longo prazo"),
            Map.entry("investimento_acessivel", "Investimento mínimo compatível com o valor disponível informado"),
            Map.entry("garantia_tesouro", "Título público, garantido pelo Tesouro Nacional"),
            Map.entry("garantia_fgc", "Garantia do FGC traz segurança adicional")
    );

    public Map<String, Integer> avaliarCriterios(RendaFixaListagemResponseDTO titulo, PerfilInvestidor perfil) {
        Map<String, Integer> criterios = new LinkedHashMap<>();
        PerfilRisco perfilRisco = PerfilRisco.valueOf(perfil.getPerfilRisco());
        ObjetivoFinanceiro objetivo = ObjetivoFinanceiro.valueOf(perfil.getObjetivo());
        HorizonteInvestimento horizonte = HorizonteInvestimento.valueOf(perfil.getHorizonte());
        Indexador indexador = Indexador.valueOf(titulo.getIndexador());

        int pesoIndexador = PESO_INDEXADOR.get(perfilRisco).get(indexador);
        if (pesoIndexador == INDEXADOR_IDEAL) {
            criterios.put("indexador_ideal", pesoIndexador);
        } else if (pesoIndexador >= 20) {
            criterios.put("indexador_adequado", pesoIndexador);
        } else {
            criterios.put("indexador_aceitavel", pesoIndexador);
        }

        int distancia = distanciaHorizonteVencimento(titulo.getVencimento(), horizonte);
        if (distancia == 0) {
            criterios.put("vencimento_no_horizonte", VENCIMENTO_NO_HORIZONTE);
        } else if (distancia == 1) {
            criterios.put("vencimento_proximo_horizonte", VENCIMENTO_PROXIMO);
        } else {
            criterios.put("vencimento_muito_alem_horizonte", VENCIMENTO_MUITO_ALEM);
        }

        avaliarLiquidez(criterios, titulo.getLiquidez(), horizonte);
        avaliarObjetivo(criterios, objetivo, indexador, titulo.isIsentoIr());

        if (perfil.getValorDisponivel() != null && titulo.getValorMinimo() != null
                && titulo.getValorMinimo().compareTo(perfil.getValorDisponivel()) <= 0) {
            criterios.put("investimento_acessivel", INVESTIMENTO_ACESSIVEL);
        }

        if (titulo.getCategoria() == CategoriaRendaFixa.TESOURO) {
            criterios.put("garantia_tesouro", GARANTIA);
        } else if (titulo.isGarantidoFgc()) {
            criterios.put("garantia_fgc", GARANTIA);
        }

        return criterios;
    }

    private void avaliarLiquidez(Map<String, Integer> criterios, String liquidez, HorizonteInvestimento horizonte) {
        if ("DIARIA".equals(liquidez)) {
            criterios.put("liquidez_diaria", LIQUIDEZ_ADEQUADA);
        } else if (horizonte == HorizonteInvestimento.LONGO_PRAZO) {
            criterios.put("liquidez_no_vencimento_longo", LIQUIDEZ_ADEQUADA);
        } else if (horizonte == HorizonteInvestimento.MEDIO_PRAZO) {
            criterios.put("liquidez_no_vencimento_medio", LIQUIDEZ_PARCIAL);
        }
    }

    private void avaliarObjetivo(Map<String, Integer> criterios, ObjetivoFinanceiro objetivo, Indexador indexador, boolean isentoIr) {
        boolean posFixado = indexador == Indexador.SELIC || indexador == Indexador.CDI;
        switch (objetivo) {
            case RENDA_PASSIVA -> {
                if (isentoIr) criterios.put("isento_ir_renda_passiva", OBJETIVO_ALINHADO);
                else criterios.put("renda_passiva_tributada", OBJETIVO_FRACO);
            }
            case PRESERVAR_CAPITAL -> {
                if (indexador == Indexador.IPCA) criterios.put("indexador_protege_inflacao", OBJETIVO_ALINHADO);
                else if (posFixado) criterios.put("pos_fixado_preservar", OBJETIVO_POS_FIXADO_PRESERVAR);
                else criterios.put("prefixado_preservar", OBJETIVO_FRACO);
            }
            case CRESCIMENTO_PATRIMONIO -> {
                if (posFixado) criterios.put("pos_fixado_crescimento", OBJETIVO_PARCIAL);
                else criterios.put("taxa_crescimento", OBJETIVO_ALINHADO);
            }
        }
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
