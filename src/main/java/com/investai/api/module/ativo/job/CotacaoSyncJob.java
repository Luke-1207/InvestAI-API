package com.investai.api.module.ativo.job;

import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.ativo.service.CotacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CotacaoSyncJob {

    private final AcaoRepository acaoRepository;
    private final CotacaoService cotacaoService;

    @Scheduled(fixedRateString = "${hgbrasil.cache.refresh-rate-ms:900000}")
    public void sincronizarCotacoes() {
        List<Acao> ativosAtivos = acaoRepository.findByAtivoTrue();

        if (ativosAtivos.isEmpty()) {
            log.debug("Nenhum ativo cadastrado para sincronizar cotações.");
            return;
        }

        log.info("Iniciando sincronização de cotações para {} ativo(s).", ativosAtivos.size());

        for (Acao acao : ativosAtivos) {
            cotacaoService.atualizarCacheSilenciosamente(acao.getCodigo());
        }

        log.info("Sincronização de cotações finalizada.");
    }
}