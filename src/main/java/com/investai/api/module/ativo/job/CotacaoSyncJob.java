package com.investai.api.module.ativo.job;

import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.ativo.service.CotacaoService;
import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.service.MonitorIntegracoesService;
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
    private final MonitorIntegracoesService monitorIntegracoesService;

    @Scheduled(fixedRateString = "${hgbrasil.cache.refresh-rate-ms:900000}")
    public void sincronizarCotacoes() {
        List<Acao> ativosAtivos = acaoRepository.findByAtivoTrue();

        if (ativosAtivos.isEmpty()) {
            log.debug("Nenhum ativo cadastrado para sincronizar cotações.");
            return;
        }

        log.info("Iniciando sincronização de cotações para {} ativo(s).", ativosAtivos.size());

        int atualizadas = 0;
        for (Acao acao : ativosAtivos) {
            if (cotacaoService.atualizarCacheSilenciosamente(acao.getCodigo())) atualizadas++;
        }

        if (atualizadas > 0) {
            monitorIntegracoesService.registrarSucesso(Integracao.HG_BRASIL);
        } else {
            monitorIntegracoesService.registrarFalha(Integracao.HG_BRASIL, "Nenhuma cotação pôde ser atualizada na HG Brasil");
        }
        log.info("Sincronização de cotações finalizada: {} de {} atualizadas.", atualizadas, ativosAtivos.size());
    }
}