package com.investai.api.module.integracao.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum Integracao {
    MICROSSERVICO_IA("Microsserviço de IA", null),
    RABBITMQ("RabbitMQ", null),
    LLM("LLM (Groq)", null),
    HG_BRASIL("HG Brasil", Duration.ofHours(72)),
    TESOURO_DIRETO("Tesouro Direto", Duration.ofHours(72)),
    BANCO_CENTRAL("Banco Central", Duration.ofHours(26));

    private final String nome;
    private final Duration limiteSemSucesso;

    public boolean verificadaAoVivo() {
        return limiteSemSucesso == null;
    }
}
