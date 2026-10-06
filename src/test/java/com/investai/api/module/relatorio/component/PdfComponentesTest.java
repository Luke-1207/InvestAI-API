package com.investai.api.module.relatorio.component;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfComponentesTest {

    private static final Path ARQUIVO_DE_TESTE = Path.of("target", "pdf-teste", "relatorio-teste.pdf");

    private final PdfHeaderComponent pdfHeaderComponent = new PdfHeaderComponent();
    private final PdfTableComponent pdfTableComponent = new PdfTableComponent();
    private final PdfDisclaimerComponent pdfDisclaimerComponent = new PdfDisclaimerComponent();

    @Test
    @DisplayName("PDF de teste - deve gerar um documento válido com cabeçalho, tabela e disclaimer")
    void pdfDeTeste_deveGerarDocumentoComOsTresComponentes() throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();

        try (Document documento = new Document(new PdfDocument(new PdfWriter(saida)))) {
            pdfHeaderComponent.adicionar(documento, "Relatório de teste", "Lucas Silva",
                    LocalDateTime.of(2026, 10, 6, 14, 30));
            documento.add(pdfTableComponent.criar(
                    new float[]{2, 5, 2, 2},
                    List.of("Código", "Nome", "Preço", "Compatibilidade"),
                    List.of(
                            List.of("TAEE11", "Transmissora Aliança de Energia Elétrica", "R$ 38,42", "MÉDIA"),
                            List.of("ITUB4", "Itaú Unibanco Holding", "R$ 34,10", "MÉDIA"),
                            List.of("HGLG11", "CSHG Logística FII", "R$ 162,55", "BAIXA"),
                            List.of("BOVA11", "iShares Ibovespa", "R$ 127,90", "BAIXA"))));
            pdfDisclaimerComponent.adicionar(documento);
        }

        byte[] pdf = saida.toByteArray();
        Files.createDirectories(ARQUIVO_DE_TESTE.getParent());
        Files.write(ARQUIVO_DE_TESTE, pdf);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

        try (PdfDocument lido = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            assertThat(lido.getNumberOfPages()).isEqualTo(1);

            String texto = PdfTextExtractor.getTextFromPage(lido.getPage(1));
            assertThat(texto)
                    .contains(PdfHeaderComponent.NOME_APLICACAO)
                    .contains("Relatório de teste")
                    .contains("Gerado para Lucas Silva")
                    .contains("06/10/2026 às 14:30")
                    .contains("Código", "Compatibilidade")
                    .contains("TAEE11", "Transmissora Aliança de Energia Elétrica", "R$ 162,55")
                    .contains(PdfDisclaimerComponent.TITULO)
                    .contains("Comissão de Valores Mobiliários");
        }
    }
}
