package com.investai.api.module.relatorio.component;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.stereotype.Component;

@Component
public class PdfSecaoComponent {

    public void adicionarTitulo(Document documento, String titulo) {
        documento.add(new Paragraph(titulo)
                .setFont(PdfEstilo.fonteNegrito())
                .setFontSize(12)
                .setFontColor(PdfEstilo.COR_MARCA)
                .setMarginTop(4)
                .setMarginBottom(6)
                .setKeepWithNext(true));
    }

    public void adicionarParagrafo(Document documento, String texto) {
        documento.add(new Paragraph(texto)
                .setFont(PdfEstilo.fonteNormal())
                .setFontSize(PdfEstilo.TAMANHO_TEXTO)
                .setFontColor(PdfEstilo.COR_TEXTO)
                .setMultipliedLeading(1.3f)
                .setMarginTop(0)
                .setMarginBottom(12));
    }

    public void adicionarNota(Document documento, String texto) {
        documento.add(new Paragraph(texto)
                .setFont(PdfEstilo.fonteNormal())
                .setFontSize(PdfEstilo.TAMANHO_TEXTO)
                .setFontColor(PdfEstilo.COR_TEXTO_SECUNDARIO)
                .setMarginTop(0)
                .setMarginBottom(12));
    }
}
