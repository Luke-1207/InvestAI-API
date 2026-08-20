package com.investai.api.infra.rabbitmq;

import com.investai.api.config.RabbitConfig;
import com.investai.api.infra.exception.IaIndisponivelException;
import com.investai.api.infra.rabbitmq.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class IaMensagemPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final IaPendingRequestStore pendingRequestStore;

    @Value("${ia.timeout-segundos:20}")
    private long timeoutSegundos;

    public RankingResponseDTO enviarRankingEAguardar(ModuloIa modulo, PerfilIaDTO perfil, List<AtivoRankingDTO> ativos) {
        String correlationId = UUID.randomUUID().toString();

        RankingRequestDTO request = RankingRequestDTO.builder()
                .correlationId(correlationId)
                .modulo(modulo)
                .perfil(perfil)
                .ativos(ativos)
                .build();

        CompletableFuture<RankingResponseDTO> future = pendingRequestStore.registrarRanking(correlationId);
        rabbitTemplate.convertAndSend(RabbitConfig.RANKING_REQUEST, request);

        try {
            return future.get(timeoutSegundos, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingRequestStore.removerRanking(correlationId);
            log.error("Timeout aguardando ranking do microsserviço IA (correlationId={})", correlationId);
            throw new IaIndisponivelException("Serviço de ranqueamento por IA não respondeu a tempo");
        } catch (InterruptedException | ExecutionException e) {
            pendingRequestStore.removerRanking(correlationId);
            Thread.currentThread().interrupt();
            log.error("Falha aguardando ranking do microsserviço IA (correlationId={}): {}", correlationId, e.getMessage());
            throw new IaIndisponivelException("Serviço de ranqueamento por IA indisponível no momento");
        }
    }

    public ResumoResponseDTO enviarResumoEAguardar(ModuloIa modulo, PerfilIaDTO perfil, Map<String, Object> ativo) {
        String correlationId = UUID.randomUUID().toString();

        ResumoRequestDTO request = ResumoRequestDTO.builder()
                .correlationId(correlationId)
                .modulo(modulo)
                .perfil(perfil)
                .ativo(ativo)
                .build();

        CompletableFuture<ResumoResponseDTO> future = pendingRequestStore.registrarResumo(correlationId);
        rabbitTemplate.convertAndSend(RabbitConfig.RESUMO_REQUEST, request);

        try {
            return future.get(timeoutSegundos, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingRequestStore.removerResumo(correlationId);
            log.error("Timeout aguardando resumo do microsserviço IA (correlationId={})", correlationId);
            throw new IaIndisponivelException("Serviço de resumo por IA não respondeu a tempo");
        } catch (InterruptedException | ExecutionException e) {
            pendingRequestStore.removerResumo(correlationId);
            Thread.currentThread().interrupt();
            log.error("Falha aguardando resumo do microsserviço IA (correlationId={}): {}", correlationId, e.getMessage());
            throw new IaIndisponivelException("Serviço de resumo por IA indisponível no momento");
        }
    }
}