package com.investai.api.module.rendafixa.dto;

import com.investai.api.module.rendafixa.entity.TipoTesouro;

import java.util.Map;

public class DescricoesTesouro {

    private DescricoesTesouro() {
    }

    public static final Map<TipoTesouro, String> TIPO = Map.of(
            TipoTesouro.SELIC, "Rende de acordo com a taxa básica de juros (Selic). É o título mais seguro e com liquidez diária, o dinheiro pode ser resgatado a qualquer momento sem perda. Ideal para reserva de emergência e perfis conservadores.",
            TipoTesouro.IPCA, "Rende a inflação (IPCA) mais uma taxa prefixada. Garante que o poder de compra do dinheiro seja preservado no longo prazo.",
            TipoTesouro.PREFIXADO, "Taxa fixada no momento da compra, você já sabe exatamente quanto vai receber no vencimento. Bom quando a expectativa é de queda de juros. Risco: se resgatar antes do prazo, pode ter rentabilidade menor que o combinado."
    );
}