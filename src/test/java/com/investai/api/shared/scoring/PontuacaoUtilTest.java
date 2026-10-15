package com.investai.api.shared.scoring;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PontuacaoUtilTest {

    @ParameterizedTest(name = "score {0} deve ser {1}")
    @CsvSource({"0,BAIXA", "49,BAIXA", "50,MEDIA", "79,MEDIA", "80,ALTA", "100,ALTA"})
    @DisplayName("classificarCompatibilidade - Alta a partir de 80 e Média a partir de 50")
    void classificarCompatibilidade_deveUsarOsNovosLimiares(int score, Compatibilidade esperada) {
        assertThat(PontuacaoUtil.classificarCompatibilidade(score)).isEqualTo(esperada);
    }

    @Test
    @DisplayName("normalizarScore - deve limitar o resultado entre 0 e 100")
    void normalizarScore_deveLimitarEntre0E100() {
        assertThat(PontuacaoUtil.normalizarScore(Map.of("a", -30, "b", 10))).isZero();
        assertThat(PontuacaoUtil.normalizarScore(Map.of("a", 80, "b", 50))).isEqualTo(100);
        assertThat(PontuacaoUtil.normalizarScore(Map.of("a", 35, "b", 25))).isEqualTo(60);
    }
}
