package com.investai.api.module.relatorio.dto;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import lombok.Builder;

@Builder
public record ItemListagemRelatorio(
        String codigo,
        String nome,
        String tipo,
        String dadoPrincipal,
        Integer score,
        Compatibilidade compatibilidade,
        String justificativa
) {
}
