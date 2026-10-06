package com.investai.api.module.relatorio.component;

import com.investai.api.module.relatorio.PdfTesteUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfDisclaimerComponentTest {

    private final PdfDisclaimerComponent pdfDisclaimerComponent = new PdfDisclaimerComponent();

    @Test
    @DisplayName("adicionar - deve escrever o título e os pontos obrigatórios do aviso")
    void adicionar_deveEscreverTituloEAviso() {
        String texto = PdfTesteUtil.extrairTexto(PdfTesteUtil.gerar(pdfDisclaimerComponent::adicionar))
                .replaceAll("\\s+", " ");

        assertThat(texto)
                .contains(PdfDisclaimerComponent.TITULO)
                .contains("não constitui recomendação")
                .contains("Comissão de Valores Mobiliários (CVM)")
                .contains("Rentabilidade passada não representa garantia de rentabilidade futura");
    }
}
