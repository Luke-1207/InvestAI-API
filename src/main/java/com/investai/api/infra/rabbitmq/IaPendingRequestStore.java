package com.investai.api.infra.rabbitmq;

import com.investai.api.infra.rabbitmq.dto.RankingResponseDTO;
import com.investai.api.infra.rabbitmq.dto.ResumoResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IaPendingRequestStore {

    private final Map<String, CompletableFuture<RankingResponseDTO>> rankingPendentes = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<ResumoResponseDTO>> resumoPendentes = new ConcurrentHashMap<>();

    public CompletableFuture<RankingResponseDTO> registrarRanking(String correlationId) {
        CompletableFuture<RankingResponseDTO> future = new CompletableFuture<>();
        rankingPendentes.put(correlationId, future);
        return future;
    }

    public void completarRanking(String correlationId, RankingResponseDTO response) {
        CompletableFuture<RankingResponseDTO> future = rankingPendentes.remove(correlationId);
        if (future != null) {
            future.complete(response);
        }
    }

    public void removerRanking(String correlationId) {
        rankingPendentes.remove(correlationId);
    }

    public CompletableFuture<ResumoResponseDTO> registrarResumo(String correlationId) {
        CompletableFuture<ResumoResponseDTO> future = new CompletableFuture<>();
        resumoPendentes.put(correlationId, future);
        return future;
    }

    public void completarResumo(String correlationId, ResumoResponseDTO response) {
        CompletableFuture<ResumoResponseDTO> future = resumoPendentes.remove(correlationId);
        if (future != null) {
            future.complete(response);
        }
    }

    public void removerResumo(String correlationId) {
        resumoPendentes.remove(correlationId);
    }
}