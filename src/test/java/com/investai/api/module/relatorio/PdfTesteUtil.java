package com.investai.api.module.relatorio;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

public final class PdfTesteUtil {

    private PdfTesteUtil() {
    }

    public static byte[] gerar(Consumer<Document> conteudo) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (Document documento = new Document(new PdfDocument(new PdfWriter(saida)))) {
            conteudo.accept(documento);
        }
        return saida.toByteArray();
    }

    public static String extrairTexto(byte[] pdf) {
        try (PdfDocument documento = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            StringBuilder texto = new StringBuilder();
            for (int pagina = 1; pagina <= documento.getNumberOfPages(); pagina++) {
                texto.append(PdfTextExtractor.getTextFromPage(documento.getPage(pagina))).append('\n');
            }
            return texto.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String extrairTextoDaPagina(byte[] pdf, int pagina) {
        try (PdfDocument documento = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            return PdfTextExtractor.getTextFromPage(documento.getPage(pagina));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int contarPaginas(byte[] pdf) {
        try (PdfDocument documento = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            return documento.getNumberOfPages();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
