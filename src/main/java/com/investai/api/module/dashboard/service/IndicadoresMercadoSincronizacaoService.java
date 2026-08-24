package com.investai.api.module.dashboard.service;

import com.investai.api.infra.bcb.BcbClient;
import com.investai.api.infra.exception.BcbIndisponivelException;
import com.investai.api.infra.exception.HgBrasilIndisponivelException;
import com.investai.api.infra.hgbrasil.HgBrasilClient;
import com.investai.api.infra.hgbrasil.dto.IndicadoresMercadoExternoDTO;
import com.investai.api.module.dashboard.dto.IndicadoresMercadoResponseDTO;
import com.investai.api.module.dashboard.entity.IndicadoresMercado;
import com.investai.api.module.dashboard.repository.IndicadoresMercadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndicadoresMercadoSincronizacaoService {

    private final IndicadoresMercadoRepository indicadoresMercadoRepository;
    private final HgBrasilClient hgBrasilClient;
    private final BcbClient bcbClient;

    @Transactional
    public void sincronizarMercado() {
        IndicadoresMercadoExternoDTO externo;
        try {
            externo = hgBrasilClient.obterIndicadoresMercado();
        } catch (HgBrasilIndisponivelException e) {
            log.warn("Sincronização de indicadores de mercado pulada — HG Brasil indisponível ({}). Mantendo dados já persistidos.", e.getMessage());
            return;
        }

        IndicadoresMercado indicadores = buscarOuCriarSnapshot();
        indicadores.setIbovespaPontos(externo.getIbovespaPontos());
        indicadores.setIbovespaVariacaoDia(externo.getIbovespaVariacaoDia());
        indicadores.setDolarValor(externo.getDolarValor());
        indicadores.setDolarVariacaoDia(externo.getDolarVariacaoDia());
        indicadores.setEuroValor(externo.getEuroValor());
        indicadores.setEuroVariacaoDia(externo.getEuroVariacaoDia());
        indicadores.setSincronizadoEm(LocalDateTime.now());

        indicadoresMercadoRepository.save(indicadores);
        log.info("Indicadores de mercado sincronizados com sucesso.");
    }

    @Transactional
    public void sincronizarSelicIpca() {
        BigDecimal selic;
        BigDecimal ipca;
        try {
            selic = bcbClient.obterSelicAtual();
            ipca = bcbClient.obterIpcaAcumulado12Meses();
        } catch (BcbIndisponivelException e) {
            log.warn("Sincronização de Selic/IPCA pulada — Banco Central indisponível ({}). Mantendo dados já persistidos.", e.getMessage());
            return;
        }

        atualizarSelicIpca(selic, ipca);
        log.info("Selic e IPCA sincronizados com o Banco Central com sucesso.");
    }

    @Transactional
    public IndicadoresMercadoResponseDTO atualizarSelicIpcaManualmente(BigDecimal selicAtual, BigDecimal ipcaAcumulado12m) {
        IndicadoresMercado indicadores = atualizarSelicIpca(selicAtual, ipcaAcumulado12m);
        return toResponseDTO(indicadores);
    }

    @Transactional(readOnly = true)
    public IndicadoresMercadoResponseDTO obterSnapshotAtual() {
        return indicadoresMercadoRepository.findTopByOrderByIdAsc()
                .map(this::toResponseDTO)
                .orElseGet(() -> IndicadoresMercadoResponseDTO.builder().build());
    }

    private IndicadoresMercado atualizarSelicIpca(BigDecimal selic, BigDecimal ipca) {
        IndicadoresMercado indicadores = buscarOuCriarSnapshot();
        indicadores.setSelicAtual(selic);
        indicadores.setIpcaAcumulado12m(ipca);
        indicadores.setSelicIpcaSincronizadoEm(LocalDateTime.now());
        return indicadoresMercadoRepository.save(indicadores);
    }

    private IndicadoresMercado buscarOuCriarSnapshot() {
        return indicadoresMercadoRepository.findTopByOrderByIdAsc()
                .orElseGet(IndicadoresMercado::new);
    }

    private IndicadoresMercadoResponseDTO toResponseDTO(IndicadoresMercado i) {
        return IndicadoresMercadoResponseDTO.builder()
                .ibovespaPontos(i.getIbovespaPontos())
                .ibovespaVariacaoDia(i.getIbovespaVariacaoDia())
                .dolarValor(i.getDolarValor())
                .dolarVariacaoDia(i.getDolarVariacaoDia())
                .euroValor(i.getEuroValor())
                .euroVariacaoDia(i.getEuroVariacaoDia())
                .selicAtual(i.getSelicAtual())
                .ipcaAcumulado12m(i.getIpcaAcumulado12m())
                .sincronizadoEm(i.getSincronizadoEm())
                .selicIpcaSincronizadoEm(i.getSelicIpcaSincronizadoEm())
                .build();
    }
}