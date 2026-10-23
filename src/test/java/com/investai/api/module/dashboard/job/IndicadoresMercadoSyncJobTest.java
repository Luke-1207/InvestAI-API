package com.investai.api.module.dashboard.job;

import com.investai.api.module.dashboard.service.IndicadoresMercadoSincronizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicadoresMercadoSyncJobTest {

    @Mock
    private IndicadoresMercadoSincronizacaoService indicadoresMercadoSincronizacaoService;

    @InjectMocks
    private IndicadoresMercadoSyncJob indicadoresMercadoSyncJob;

    @Test
    @DisplayName("sincronizarMercado - deve delegar para o service de sincronização de mercado")
    void sincronizarMercado_deveDelegarParaOServiceDeMercado() {
        indicadoresMercadoSyncJob.sincronizarMercado();

        verify(indicadoresMercadoSincronizacaoService).sincronizarMercado();
    }

    @Test
    @DisplayName("sincronizarSelicIpca - deve delegar para o service de sincronização de Selic/IPCA")
    void sincronizarSelicIpca_deveDelegarParaOServiceDeSelicIpca() {
        indicadoresMercadoSyncJob.sincronizarSelicIpca();

        verify(indicadoresMercadoSincronizacaoService).sincronizarSelicIpca();
    }

    @Test
    @DisplayName("sincronizarNaSubida - deve buscar mercado e Selic/IPCA quando nunca foram sincronizados")
    void sincronizarNaSubida_bancoNovo_deveSincronizarTudo() {
        when(indicadoresMercadoSincronizacaoService.selicIpcaDesatualizados()).thenReturn(true);

        indicadoresMercadoSyncJob.sincronizarNaSubida();

        verify(indicadoresMercadoSincronizacaoService).sincronizarMercado();
        verify(indicadoresMercadoSincronizacaoService).sincronizarSelicIpca();
    }

    @Test
    @DisplayName("sincronizarNaSubida - não deve sobrescrever Selic/IPCA atualizados nas últimas 24 horas")
    void sincronizarNaSubida_selicIpcaRecentes_devePreservar() {
        when(indicadoresMercadoSincronizacaoService.selicIpcaDesatualizados()).thenReturn(false);

        indicadoresMercadoSyncJob.sincronizarNaSubida();

        verify(indicadoresMercadoSincronizacaoService).sincronizarMercado();
        verify(indicadoresMercadoSincronizacaoService, never()).sincronizarSelicIpca();
    }

    @Test
    @DisplayName("sincronizarNaSubida - erro inesperado não deve derrubar a subida da aplicação")
    void sincronizarNaSubida_erroInesperado_naoDevePropagar() {
        doThrow(new IllegalStateException("banco fora")).when(indicadoresMercadoSincronizacaoService).sincronizarMercado();

        assertThatCode(() -> indicadoresMercadoSyncJob.sincronizarNaSubida()).doesNotThrowAnyException();
    }
}
