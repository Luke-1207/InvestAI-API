package com.investai.api.infra.rabbitmq;

import com.investai.api.config.RabbitConfig;
import com.investai.api.infra.rabbitmq.dto.RankingResponseDTO;
import com.investai.api.infra.rabbitmq.dto.ResumoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IaMensagemConsumer {

    private final IaPendingRequestStore pendingRequestStore;

    @RabbitListener(queues = RabbitConfig.RANKING_RESPONSE)
    public void receberRanking(RankingResponseDTO response) {
        log.debug("Resposta de ranking recebida do microsserviço IA (correlationId={})", response.getCorrelationId());
        pendingRequestStore.completarRanking(response.getCorrelationId(), response);
    }

    @RabbitListener(queues = RabbitConfig.RESUMO_RESPONSE)
    public void receberResumo(ResumoResponseDTO response) {
        log.debug("Resumo recebido do microsserviço IA (correlationId={})", response.getCorrelationId());
        pendingRequestStore.completarResumo(response.getCorrelationId(), response);
    }
}