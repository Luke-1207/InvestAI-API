package com.investai.api.module.relatorio.component;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PdfTableComponent {

    private static final String VALOR_VAZIO = "-";
    private static final float ESPACAMENTO_CELULA = 6f;

    public Table criar(List<String> cabecalhos, List<List<String>> linhas) {
        float[] largurasIguais = new float[cabecalhos.size()];
        Arrays.fill(largurasIguais, 1f);
        return criar(largurasIguais, cabecalhos, linhas);
    }

    public Table criar(float[] largurasRelativas, List<String> cabecalhos, List<List<String>> linhas) {
        if (cabecalhos.isEmpty()) {
            throw new IllegalArgumentException("A tabela precisa de ao menos uma coluna");
        }
        if (largurasRelativas.length != cabecalhos.size()) {
            throw new IllegalArgumentException("A quantidade de larguras deve ser igual à de colunas");
        }

        PdfFont normal = PdfEstilo.fonteNormal();
        PdfFont negrito = PdfEstilo.fonteNegrito();

        Table tabela = new Table(UnitValue.createPercentArray(largurasRelativas))
                .useAllAvailableWidth()
                .setMarginBottom(16);

        for (String cabecalho : cabecalhos) {
            tabela.addHeaderCell(new Cell()
                    .setBackgroundColor(PdfEstilo.COR_MARCA)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(ESPACAMENTO_CELULA)
                    .add(new Paragraph(cabecalho)
                            .setFont(negrito)
                            .setFontSize(PdfEstilo.TAMANHO_TEXTO)
                            .setFontColor(PdfEstilo.COR_SOBRE_MARCA)
                            .setMargin(0)));
        }

        for (int indice = 0; indice < linhas.size(); indice++) {
            List<String> linha = linhas.get(indice);
            if (linha.size() != cabecalhos.size()) {
                throw new IllegalArgumentException(
                        "A linha " + (indice + 1) + " tem " + linha.size() + " valores, mas a tabela tem "
                                + cabecalhos.size() + " colunas");
            }

            boolean alternada = indice % 2 == 1;
            for (String valor : linha) {
                Cell celula = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .setBorderBottom(new SolidBorder(PdfEstilo.COR_BORDA, 0.5f))
                        .setPadding(ESPACAMENTO_CELULA)
                        .add(new Paragraph(valor == null || valor.isBlank() ? VALOR_VAZIO : valor)
                                .setFont(normal)
                                .setFontSize(PdfEstilo.TAMANHO_TEXTO)
                                .setFontColor(PdfEstilo.COR_TEXTO)
                                .setMargin(0));
                if (alternada) {
                    celula.setBackgroundColor(PdfEstilo.COR_LINHA_ALTERNADA);
                }
                tabela.addCell(celula);
            }
        }

        return tabela;
    }
}
