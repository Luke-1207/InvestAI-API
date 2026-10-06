package com.investai.api.module.relatorio.builder;

import com.investai.api.module.perfil.dto.ValorDescritoDTO;

import java.util.Map;

public final class RotulosPerfil {

    private static final Map<String, String> ROTULOS = Map.of(
            "CONSERVADOR", "Conservador",
            "MODERADO", "Moderado",
            "ARROJADO", "Arrojado",
            "RENDA_PASSIVA", "Renda passiva",
            "CRESCIMENTO_PATRIMONIO", "Crescimento de patrimônio",
            "PRESERVAR_CAPITAL", "Preservar capital",
            "CURTO_PRAZO", "Curto prazo (menos de 1 ano)",
            "MEDIO_PRAZO", "Médio prazo (1 a 5 anos)",
            "LONGO_PRAZO", "Longo prazo (mais de 5 anos)");

    private RotulosPerfil() {
    }

    public static String de(ValorDescritoDTO campo) {
        if (campo == null || campo.getValor() == null) {
            return null;
        }
        return ROTULOS.getOrDefault(campo.getValor(), campo.getValor());
    }
}
