package com.investai.api.module.dashboard.dto;

public enum NivelUrgencia {
    ALTA,
    MEDIA,
    BAIXA;

    private static final long LIMITE_ALTA_DIAS = 7;
    private static final long LIMITE_MEDIA_DIAS = 15;

    public static NivelUrgencia porDiasRestantes(long dias) {
        if (dias <= LIMITE_ALTA_DIAS) {
            return ALTA;
        }
        if (dias <= LIMITE_MEDIA_DIAS) {
            return MEDIA;
        }
        return BAIXA;
    }
}
