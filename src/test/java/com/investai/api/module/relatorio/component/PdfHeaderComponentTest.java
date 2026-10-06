package com.investai.api.module.relatorio.component;

import com.investai.api.module.relatorio.PdfTesteUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PdfHeaderComponentTest {

    private final PdfHeaderComponent pdfHeaderComponent = new PdfHeaderComponent();

    @Test
    @DisplayName("adicionar - deve escrever nome da aplicação, título, usuário e data formatada")
    void adicionar_deveEscreverIdentificacaoEEmissao() {
        byte[] pdf = PdfTesteUtil.gerar(documento -> pdfHeaderComponent.adicionar(
                documento, "Análise de ativo: TAEE11", "Lucas Silva", LocalDateTime.of(2026, 1, 5, 9, 7)));

        assertThat(PdfTesteUtil.extrairTexto(pdf))
                .contains(PdfHeaderComponent.NOME_APLICACAO)
                .contains("Análise de ativo: TAEE11")
                .contains("Gerado para Lucas Silva")
                .contains("Em 05/01/2026 às 09:07");
    }
}
