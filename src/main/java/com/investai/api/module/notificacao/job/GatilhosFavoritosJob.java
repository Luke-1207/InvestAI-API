package com.investai.api.module.notificacao.job;

import com.investai.api.module.notificacao.service.GatilhosFavoritosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatilhosFavoritosJob {

    private final GatilhosFavoritosService gatilhosFavoritosService;

    @Scheduled(
            initialDelayString = "${notificacoes.gatilhos.initial-delay-ms:120000}",
            fixedRateString = "${notificacoes.gatilhos.intervalo-ms:900000}"
    )
    public void verificar() {
        try {
            int geradas = gatilhosFavoritosService.verificar(LocalDateTime.now());
            if (geradas > 0) {
                log.info("Verificação de gatilhos dos favoritos gerou {} notificação(ões).", geradas);
            }
        } catch (Exception e) {
            log.error("Falha na verificação de gatilhos dos favoritos: {}", e.getMessage(), e);
        }
    }
}
