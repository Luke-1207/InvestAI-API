package com.investai.api.module.relatorio.builder;

import com.investai.api.module.rendafixa.entity.Indexador;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FormatoRelatorio {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private FormatoRelatorio() {
    }

    public static String moeda(BigDecimal valor) {
        return valor == null ? null : "R$ " + decimal().format(valor);
    }

    public static String percentual(BigDecimal valor) {
        return valor == null ? null : decimal().format(valor) + "%";
    }

    public static String percentualComSinal(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        return (valor.signum() > 0 ? "+" : "") + decimal().format(valor) + "%";
    }

    public static String numero(BigDecimal valor) {
        return valor == null ? null : decimal().format(valor);
    }

    public static String taxaComIndexador(Indexador indexador, BigDecimal taxa, boolean taxaSomadaAoIndexador) {
        if (taxa == null) {
            return null;
        }
        String valor = decimal().format(taxa) + "%";
        if (indexador == null) {
            return valor;
        }
        return switch (indexador) {
            case CDI -> valor + " do CDI";
            case SELIC -> taxaSomadaAoIndexador ? "Selic + " + valor + " a.a." : valor + " da Selic";
            case IPCA -> "IPCA + " + valor + " a.a.";
            case PREFIXADO -> valor + " a.a.";
        };
    }

    public static String telefone(String digitos) {
        if (digitos == null || digitos.isBlank()) {
            return null;
        }
        if (digitos.length() == 11) {
            return "(" + digitos.substring(0, 2) + ") " + digitos.substring(2, 7) + "-" + digitos.substring(7);
        }
        if (digitos.length() == 10) {
            return "(" + digitos.substring(0, 2) + ") " + digitos.substring(2, 6) + "-" + digitos.substring(6);
        }
        return digitos;
    }

    public static String data(LocalDate valor) {
        return valor == null ? null : valor.format(DATA);
    }

    public static String dataHora(LocalDateTime valor) {
        return valor == null ? null : valor.format(DATA_HORA);
    }

    private static DecimalFormat decimal() {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
    }
}
