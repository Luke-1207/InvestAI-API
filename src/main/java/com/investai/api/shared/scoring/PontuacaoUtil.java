package com.investai.api.shared.scoring;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PontuacaoUtil {

    public static final int LIMIAR_ALTA = 80;
    public static final int LIMIAR_MEDIA = 50;

    private PontuacaoUtil() {
    }

    public static int normalizarScore(Map<String, Integer> criterios) {
        int scoreBruto = criterios.values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(0, Math.min(100, scoreBruto));
    }

    public static Compatibilidade classificarCompatibilidade(int score) {
        if (score >= LIMIAR_ALTA) return Compatibilidade.ALTA;
        if (score >= LIMIAR_MEDIA) return Compatibilidade.MEDIA;
        return Compatibilidade.BAIXA;
    }

    public static List<CriterioScoreDTO> listarCriterios(Map<String, Integer> criterios, Map<String, String> descricoes) {
        return criterios.entrySet().stream()
                .filter(e -> e.getValue() != 0 && descricoes.containsKey(e.getKey()))
                .map(e -> new CriterioScoreDTO(e.getKey(), descricoes.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingInt(CriterioScoreDTO::getPontos).reversed())
                .toList();
    }

    public static String gerarJustificativa(Map<String, Integer> criterios, Map<String, String> templates) {
        List<String> frases = criterios.entrySet().stream()
                .sorted((a, b) -> Integer.compare(Math.abs(b.getValue()), Math.abs(a.getValue())))
                .limit(2)
                .map(e -> templates.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();

        if (frases.isEmpty()) {
            return "Dentro dos critérios mínimos avaliados para o seu perfil.";
        }
        return String.join(". ", frases) + ".";
    }
}