package com.investai.api.module.ativo.dto;

import com.investai.api.infra.exception.BusinessException;
import lombok.Getter;

@Getter
public enum PeriodoHistorico {

    UMA_SEMANA("1S", 7),
    UM_MES("1M", 30),
    TRES_MESES("3M", 90),
    SEIS_MESES("6M", 180),
    UM_ANO("1A", 365);

    private final String codigo;
    private final int diasAtras;

    PeriodoHistorico(String codigo, int diasAtras) {
        this.codigo = codigo;
        this.diasAtras = diasAtras;
    }

    public static PeriodoHistorico fromCodigo(String codigo) {
        for (PeriodoHistorico periodo : values()) {
            if (periodo.codigo.equalsIgnoreCase(codigo)) {
                return periodo;
            }
        }
        throw new BusinessException(
                "Período inválido: " + codigo + ". Valores aceitos: 1S, 1M, 3M, 6M, 1A");
    }
}
