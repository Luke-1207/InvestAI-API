package com.investai.api.module.dashboard.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.investai.api.module.dashboard.dto.DashboardResponseDTO;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.service.PerfilService;
import com.investai.api.shared.event.PerfilAlteradoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IndicadoresMercadoSincronizacaoService indicadoresMercadoSincronizacaoService;
    private final DashboardSugestaoService dashboardSugestaoService;
    private final PerfilService perfilService;

    @Qualifier("dashboardExecutor")
    private final Executor dashboardExecutor;

    private final Cache<UUID, DashboardResponseDTO> cache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public DashboardResponseDTO obterDashboard(UUID usuarioId) {
        return cache.get(usuarioId, this::montarDashboard);
    }

    private DashboardResponseDTO montarDashboard(UUID usuarioId) {
        CompletableFuture<com.investai.api.module.dashboard.dto.IndicadoresMercadoResponseDTO> indicadoresFuture =
                CompletableFuture.supplyAsync(indicadoresMercadoSincronizacaoService::obterSnapshotAtual, dashboardExecutor);

        CompletableFuture<com.investai.api.module.dashboard.dto.SugestoesRendaVariavelResponseDTO> rendaVariavelFuture =
                CompletableFuture.supplyAsync(() -> dashboardSugestaoService.sugerirRendaVariavel(usuarioId), dashboardExecutor);

        CompletableFuture<com.investai.api.module.dashboard.dto.SugestoesRendaFixaResponseDTO> rendaFixaFuture =
                CompletableFuture.supplyAsync(() -> dashboardSugestaoService.sugerirRendaFixa(usuarioId), dashboardExecutor);

        CompletableFuture<PerfilResponseDTO> perfilFuture =
                CompletableFuture.supplyAsync(() -> perfilService.obterPerfil(usuarioId), dashboardExecutor);

        CompletableFuture.allOf(indicadoresFuture, rendaVariavelFuture, rendaFixaFuture, perfilFuture).join();

        return DashboardResponseDTO.builder()
                .indicadoresMercado(indicadoresFuture.join())
                .sugestoesRendaVariavel(rendaVariavelFuture.join())
                .sugestoesRendaFixa(rendaFixaFuture.join())
                .perfil(perfilFuture.join())
                .geradoEm(LocalDateTime.now())
                .build();
    }

    @EventListener
    public void aoAlterarPerfil(PerfilAlteradoEvent event) {
        cache.invalidate(event.getUsuarioId());
    }
}