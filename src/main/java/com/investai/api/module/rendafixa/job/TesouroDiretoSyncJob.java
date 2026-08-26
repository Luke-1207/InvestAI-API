package com.investai.api.module.rendafixa.job;

import com.investai.api.module.rendafixa.service.TesouroDiretoSincronizacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TesouroDiretoSyncJob {

    private final TesouroDiretoSincronizacaoService tesouroDiretoSincronizacaoService;

    @Scheduled(cron = "${tesourodireto.sync.cron:0 0,30 10-18 * * MON-FRI}")
    public void sincronizarTesouroDireto() {
        log.info("Iniciando sincronização agendada do Tesouro Direto.");
        tesouroDiretoSincronizacaoService.sincronizar();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarNaSubida() {
        log.info("Sincronização inicial do Tesouro Direto ao subir a aplicação.");
        tesouroDiretoSincronizacaoService.sincronizar();
    }
}