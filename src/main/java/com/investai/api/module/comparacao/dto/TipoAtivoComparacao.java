package com.investai.api.module.comparacao.dto;

public enum TipoAtivoComparacao {
    ACAO(true),
    FII(true),
    ETF(true),
    TESOURO(false),
    CDB(false),
    LCI(false),
    LCA(false);

    private final boolean rendaVariavel;

    TipoAtivoComparacao(boolean rendaVariavel) {
        this.rendaVariavel = rendaVariavel;
    }

    public boolean isRendaVariavel() {
        return rendaVariavel;
    }
}
