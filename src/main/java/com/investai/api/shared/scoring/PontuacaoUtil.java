package com.investai.api.shared.scoring;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PontuacaoUtil {

    private PontuacaoUtil() {
    }

    public static int normalizarScore(Map<String, Integer> criterios) {
        int scoreBruto = criterios.values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(0, Math.min(100, scoreBruto));
    }

    public static Compatibilidade classificarCompatibilidade(int score) {
        if (score >= 70) return Compatibilidade.ALTA;
        if (score >= 40) return Compatibilidade.MEDIA;
        return Compatibilidade.BAIXA;
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