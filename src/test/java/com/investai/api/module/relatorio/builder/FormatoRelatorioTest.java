package com.investai.api.module.relatorio.builder;

import com.investai.api.module.rendafixa.entity.Indexador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FormatoRelatorioTest {

    @Test
    @DisplayName("moeda - deve formatar em reais com separadores do Brasil")
    void moeda_deveFormatarEmReais() {
        assertThat(FormatoRelatorio.moeda(new BigDecimal("15000"))).isEqualTo("R$ 15.000,00");
        assertThat(FormatoRelatorio.moeda(new BigDecimal("38.425"))).isEqualTo("R$ 38,42");
    }

    @Test
    @DisplayName("percentual - deve formatar com duas casas, e com sinal só quando positivo")
    void percentual_deveFormatarComESemSinal() {
        assertThat(FormatoRelatorio.percentual(new BigDecimal("8.4"))).isEqualTo("8,40%");
        assertThat(FormatoRelatorio.percentualComSinal(new BigDecimal("1.23"))).isEqualTo("+1,23%");
        assertThat(FormatoRelatorio.percentualComSinal(new BigDecimal("-1.2"))).isEqualTo("-1,20%");
        assertThat(FormatoRelatorio.percentualComSinal(BigDecimal.ZERO)).isEqualTo("0,00%");
    }

    @Test
    @DisplayName("numero, data e dataHora - devem formatar no padrão brasileiro")
    void numeroEDatas_devemFormatarNoPadraoBrasileiro() {
        assertThat(FormatoRelatorio.numero(new BigDecimal("1.35"))).isEqualTo("1,35");
        assertThat(FormatoRelatorio.data(LocalDate.of(2028, 5, 10))).isEqualTo("10/05/2028");
        assertThat(FormatoRelatorio.dataHora(LocalDateTime.of(2026, 10, 6, 14, 30))).isEqualTo("06/10/2026 às 14:30");
    }

    @Test
    @DisplayName("telefone - deve formatar celular e fixo, e devolver como veio se o tamanho for outro")
    void telefone_deveFormatarCelularEFixo() {
        assertThat(FormatoRelatorio.telefone("19999998888")).isEqualTo("(19) 99999-8888");
        assertThat(FormatoRelatorio.telefone("1933334444")).isEqualTo("(19) 3333-4444");
        assertThat(FormatoRelatorio.telefone("12345")).isEqualTo("12345");
        assertThat(FormatoRelatorio.telefone(" ")).isNull();
        assertThat(FormatoRelatorio.telefone(null)).isNull();
    }

    @Test
    @DisplayName("todos os formatadores devem devolver null quando o valor é null")
    void valorNulo_deveDevolverNull() {
        assertThat(FormatoRelatorio.moeda(null)).isNull();
        assertThat(FormatoRelatorio.percentual(null)).isNull();
        assertThat(FormatoRelatorio.percentualComSinal(null)).isNull();
        assertThat(FormatoRelatorio.numero(null)).isNull();
        assertThat(FormatoRelatorio.data(null)).isNull();
        assertThat(FormatoRelatorio.dataHora(null)).isNull();
        assertThat(FormatoRelatorio.taxaComIndexador(Indexador.CDI, null, false)).isNull();
    }

    @Test
    @DisplayName("taxaComIndexador - deve escrever a taxa na unidade de cada indexador")
    void taxaComIndexador_deveEscreverNaUnidadeDoIndexador() {
        BigDecimal taxa = new BigDecimal("110");

        assertThat(FormatoRelatorio.taxaComIndexador(Indexador.CDI, taxa, false)).isEqualTo("110,00% do CDI");
        assertThat(FormatoRelatorio.taxaComIndexador(Indexador.PREFIXADO, new BigDecimal("12.5"), false)).isEqualTo("12,50% a.a.");
        assertThat(FormatoRelatorio.taxaComIndexador(Indexador.IPCA, new BigDecimal("5.5"), true)).isEqualTo("IPCA + 5,50% a.a.");
        assertThat(FormatoRelatorio.taxaComIndexador(Indexador.SELIC, new BigDecimal("0.07"), true)).isEqualTo("Selic + 0,07% a.a.");
        assertThat(FormatoRelatorio.taxaComIndexador(Indexador.SELIC, new BigDecimal("100"), false)).isEqualTo("100,00% da Selic");
        assertThat(FormatoRelatorio.taxaComIndexador(null, taxa, false)).isEqualTo("110,00%");
    }
}
