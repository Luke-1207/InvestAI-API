package com.investai.api.module.relatorio.component;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PdfHeaderComponent {

    public static final String NOME_APLICACAO = "InvestAI";

    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    public void adicionar(Document documento, String tituloRelatorio, String nomeUsuario, LocalDateTime geradoEm) {
        PdfFont normal = PdfEstilo.fonteNormal();
        PdfFont negrito = PdfEstilo.fonteNegrito();

        Cell identificacao = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.BOTTOM)
                .add(new Paragraph(NOME_APLICACAO)
                        .setFont(negrito)
                        .setFontSize(22)
                        .setFontColor(PdfEstilo.COR_MARCA)
                        .setMargin(0))
                .add(new Paragraph(tituloRelatorio)
                        .setFont(negrito)
                        .setFontSize(13)
                        .setFontColor(PdfEstilo.COR_TEXTO)
                        .setMargin(0));

        Cell emissao = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.BOTTOM)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(linhaEmissao("Gerado para " + nomeUsuario, normal))
                .add(linhaEmissao("Em " + geradoEm.format(FORMATO_DATA_HORA), normal));

        Table cabecalho = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth()
                .addCell(identificacao)
                .addCell(emissao);

        SolidLine linha = new SolidLine(1.5f);
        linha.setColor(PdfEstilo.COR_MARCA);

        documento.add(cabecalho);
        documento.add(new LineSeparator(linha).setMarginTop(8).setMarginBottom(16));
    }

    private Paragraph linhaEmissao(String texto, PdfFont fonte) {
        return new Paragraph(texto)
                .setFont(fonte)
                .setFontSize(PdfEstilo.TAMANHO_TEXTO_PEQUENO)
                .setFontColor(PdfEstilo.COR_TEXTO_SECUNDARIO)
                .setMargin(0);
    }
}
