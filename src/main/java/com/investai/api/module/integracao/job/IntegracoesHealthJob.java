package com.investai.api.module.integracao.job;

import com.investai.api.module.integracao.service.SaudeIntegracoesService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegracoesHealthJob {

    private final SaudeIntegracoesService saudeIntegracoesService;

    @Scheduled(
            initialDelayString = "${integracoes.health.initial-delay-ms:60000}",
            fixedRateString = "${integracoes.health.refresh-rate-ms:300000}"
    )
    public void verificar() {
        saudeIntegracoesService.verificar();
    }
}
