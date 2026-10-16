package com.investai.api.shared.scoring;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.LinkedHashMap;
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
    @DisplayName("listarCriterios - deve listar do que mais somou ao que mais tirou, ignorando zeros e chaves sem descrição")
    void listarCriterios_deveOrdenarEFiltrar() {
        Map<String, Integer> criterios = new LinkedHashMap<>();
        criterios.put("setor_evitado", -20);
        criterios.put("preco", 10);
        criterios.put("zero", 0);
        criterios.put("sem_descricao", 5);
        criterios.put("volatilidade", 35);
        Map<String, String> descricoes = Map.of(
                "setor_evitado", "Setor evitado", "preco", "Preço acessível", "zero", "Zero", "volatilidade", "Oscilação ideal");

        assertThat(PontuacaoUtil.listarCriterios(criterios, descricoes))
                .extracting(CriterioScoreDTO::getChave, CriterioScoreDTO::getPontos)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("volatilidade", 35),
                        org.assertj.core.groups.Tuple.tuple("preco", 10),
                        org.assertj.core.groups.Tuple.tuple("setor_evitado", -20));
    }

    @Test
    @DisplayName("normalizarScore - deve limitar o resultado entre 0 e 100")
    void normalizarScore_deveLimitarEntre0E100() {
        assertThat(PontuacaoUtil.normalizarScore(Map.of("a", -30, "b", 10))).isZero();
        assertThat(PontuacaoUtil.normalizarScore(Map.of("a", 80, "b", 50))).isEqualTo(100);
        assertThat(PontuacaoUtil.normalizarScore(Map.of("a", 35, "b", 25))).isEqualTo(60);
    }
}
