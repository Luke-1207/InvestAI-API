package com.investai.api.module.relatorio.component;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.stereotype.Component;

@Component
public class PdfDisclaimerComponent {

    public static final String TITULO = "Aviso importante";

    public static final String TEXTO = "Este relatório tem caráter exclusivamente informativo e educacional. "
            + "Ele não constitui recomendação, oferta ou solicitação de compra ou venda de valores mobiliários "
            + "e não substitui a orientação de um consultor ou analista de valores mobiliários autorizado pela "
            + "Comissão de Valores Mobiliários (CVM). As pontuações, análises e resumos são gerados de forma "
            + "automatizada, inclusive com uso de inteligência artificial, a partir do perfil informado pelo "
            + "usuário e de dados públicos de mercado, e podem conter imprecisões. Rentabilidade passada não "
            + "representa garantia de rentabilidade futura. Toda decisão de investimento é de responsabilidade "
            + "exclusiva do investidor.";

    public void adicionar(Document documento) {
        Div bloco = new Div()
                .setBackgroundColor(PdfEstilo.COR_FUNDO_AVISO)
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(PdfEstilo.COR_BORDA_AVISO, 3f))
                .setPadding(10)
                .setMarginTop(16)
                .setKeepTogether(true)
                .add(new Paragraph(TITULO)
                        .setFont(PdfEstilo.fonteNegrito())
                        .setFontSize(PdfEstilo.TAMANHO_TEXTO)
                        .setFontColor(PdfEstilo.COR_TEXTO)
                        .setMargin(0)
                        .setMarginBottom(4))
                .add(new Paragraph(TEXTO)
                        .setFont(PdfEstilo.fonteNormal())
                        .setFontSize(PdfEstilo.TAMANHO_TEXTO_PEQUENO)
                        .setFontColor(PdfEstilo.COR_TEXTO_SECUNDARIO)
                        .setMargin(0));

        documento.add(bloco);
    }
}
