package com.investai.api.module.dashboard.job;

import com.investai.api.module.dashboard.service.IndicadoresMercadoSincronizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

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
}