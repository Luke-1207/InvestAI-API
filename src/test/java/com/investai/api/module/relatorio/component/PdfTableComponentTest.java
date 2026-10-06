package com.investai.api.module.relatorio.component;

import com.investai.api.module.relatorio.PdfTesteUtil;
import com.itextpdf.layout.element.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfTableComponentTest {

    private final PdfTableComponent pdfTableComponent = new PdfTableComponent();

    @Test
    @DisplayName("criar - deve montar a tabela com cabeçalhos e valores")
    void criar_deveMontarTabelaComCabecalhosEValores() {
        Table tabela = pdfTableComponent.criar(
                List.of("Código", "Preço"),
                List.of(List.of("TAEE11", "R$ 38,42"), List.of("ITUB4", "R$ 34,10")));

        assertThat(tabela.getNumberOfColumns()).isEqualTo(2);
        assertThat(tabela.getNumberOfRows()).isEqualTo(2);
        assertThat(PdfTesteUtil.extrairTexto(PdfTesteUtil.gerar(documento -> documento.add(tabela))))
                .contains("Código", "Preço", "TAEE11", "R$ 38,42", "ITUB4", "R$ 34,10");
    }

    @Test
    @DisplayName("criar - valor nulo ou em branco deve virar traço")
    void criar_valorNuloOuEmBranco_deveVirarTraco() {
        Table tabela = pdfTableComponent.criar(
                List.of("Indicador", "Valor"),
                List.of(Arrays.asList("P/L", null), List.of("P/VP", "  ")));

        String texto = PdfTesteUtil.extrairTexto(PdfTesteUtil.gerar(documento -> documento.add(tabela)));

        assertThat(texto).containsPattern("P/L\\s+-").containsPattern("P/VP\\s+-");
    }

    @Test
    @DisplayName("criar - tabela que quebra de página deve repetir o cabeçalho")
    void criar_tabelaEmMaisDeUmaPagina_deveRepetirCabecalho() {
        List<List<String>> linhas = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            linhas.add(List.of("ATIVO" + i, "R$ " + i + ",00"));
        }
        Table tabela = pdfTableComponent.criar(List.of("Código do ativo", "Preço"), linhas);

        byte[] pdf = PdfTesteUtil.gerar(documento -> documento.add(tabela));

        assertThat(PdfTesteUtil.contarPaginas(pdf)).isGreaterThan(1);
        assertThat(PdfTesteUtil.extrairTextoDaPagina(pdf, 2)).contains("Código do ativo");
    }

    @Test
    @DisplayName("criar - tamanho de fonte menor deve gerar uma tabela mais baixa")
    void criar_tamanhoDeFonteMenor_deveOcuparMenosPaginas() {
        List<List<String>> linhas = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            linhas.add(List.of("ATIVO" + i, "R$ " + i + ",00"));
        }
        float[] larguras = {1, 1};
        List<String> cabecalhos = List.of("Código", "Preço");

        int paginasPadrao = PdfTesteUtil.contarPaginas(PdfTesteUtil.gerar(
                documento -> documento.add(pdfTableComponent.criar(larguras, cabecalhos, linhas))));
        int paginasFonteMenor = PdfTesteUtil.contarPaginas(PdfTesteUtil.gerar(
                documento -> documento.add(pdfTableComponent.criar(larguras, cabecalhos, linhas, 5f))));

        assertThat(paginasFonteMenor).isLessThan(paginasPadrao);
    }

    @Test
    @DisplayName("criar - sem colunas deve lançar IllegalArgumentException")
    void criar_semColunas_deveLancarExcecao() {
        assertThatThrownBy(() -> pdfTableComponent.criar(List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ao menos uma coluna");
    }

    @Test
    @DisplayName("criar - quantidade de larguras diferente da de colunas deve lançar IllegalArgumentException")
    void criar_largurasIncompativeis_deveLancarExcecao() {
        assertThatThrownBy(() -> pdfTableComponent.criar(new float[]{1, 2, 3}, List.of("A", "B"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("larguras");
    }

    @Test
    @DisplayName("criar - linha com colunas a menos deve lançar IllegalArgumentException indicando a linha")
    void criar_linhaComColunasAMenos_deveLancarExcecao() {
        assertThatThrownBy(() -> pdfTableComponent.criar(
                List.of("A", "B"),
                List.of(List.of("1", "2"), List.of("só um"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("linha 2");
    }
}
