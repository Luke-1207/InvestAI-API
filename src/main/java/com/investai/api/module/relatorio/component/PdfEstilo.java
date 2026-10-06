package com.investai.api.module.relatorio.component;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class PdfEstilo {

    public static final Color COR_MARCA = new DeviceRgb(82, 79, 224);
    public static final Color COR_TEXTO = new DeviceRgb(24, 24, 27);
    public static final Color COR_TEXTO_SECUNDARIO = new DeviceRgb(82, 82, 91);
    public static final Color COR_BORDA = new DeviceRgb(212, 212, 216);
    public static final Color COR_LINHA_ALTERNADA = new DeviceRgb(244, 244, 246);
    public static final Color COR_FUNDO_AVISO = new DeviceRgb(255, 247, 237);
    public static final Color COR_BORDA_AVISO = new DeviceRgb(217, 119, 6);
    public static final Color COR_SOBRE_MARCA = new DeviceRgb(255, 255, 255);

    public static final float TAMANHO_TEXTO = 10f;
    public static final float TAMANHO_TEXTO_PEQUENO = 8.5f;

    private PdfEstilo() {
    }

    public static PdfFont fonteNormal() {
        return criarFonte(StandardFonts.HELVETICA);
    }

    public static PdfFont fonteNegrito() {
        return criarFonte(StandardFonts.HELVETICA_BOLD);
    }

    private static PdfFont criarFonte(String nome) {
        try {
            return PdfFontFactory.createFont(nome);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível carregar a fonte " + nome, e);
        }
    }
}
