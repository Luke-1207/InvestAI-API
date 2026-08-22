package com.investai.api.module.dashboard.job;

import com.investai.api.module.dashboard.service.IndicadoresMercadoSincronizacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndicadoresMercadoSyncJob {

    private final IndicadoresMercadoSincronizacaoService indicadoresMercadoSincronizacaoService;

    @Scheduled(cron = "${mercado.sync.cron:0 */15 10-18 * * MON-FRI}")
    public void sincronizarMercado() {
        log.info("Iniciando sincronização agendada de indicadores de mercado.");
        indicadoresMercadoSincronizacaoService.sincronizarMercado();
    }

    @Scheduled(cron = "${mercado.selic-ipca.sync.cron:0 0 7 * * *}")
    public void sincronizarSelicIpca() {
        log.info("Iniciando sincronização agendada de Selic/IPCA.");
        indicadoresMercadoSincronizacaoService.sincronizarSelicIpca();
    }
}