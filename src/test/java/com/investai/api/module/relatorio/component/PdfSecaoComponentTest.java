package com.investai.api.module.relatorio.component;

import com.investai.api.module.relatorio.PdfTesteUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfSecaoComponentTest {

    private final PdfSecaoComponent pdfSecaoComponent = new PdfSecaoComponent();

    @Test
    @DisplayName("deve escrever título, parágrafo e nota na ordem em que foram adicionados")
    void deveEscreverTituloParagrafoENota() {
        String texto = PdfTesteUtil.extrairTexto(PdfTesteUtil.gerar(documento -> {
            pdfSecaoComponent.adicionarTitulo(documento, "Indicadores do ativo");
            pdfSecaoComponent.adicionarParagrafo(documento, "Texto do parágrafo");
            pdfSecaoComponent.adicionarNota(documento, "Texto da nota");
        }));

        assertThat(texto).containsSubsequence("Indicadores do ativo", "Texto do parágrafo", "Texto da nota");
    }
}
