package com.investai.api.module.rendafixa.job;

import com.investai.api.module.rendafixa.service.TesouroDiretoSincronizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TesouroDiretoSyncJobTest {

    @Mock
    private TesouroDiretoSincronizacaoService tesouroDiretoSincronizacaoService;

    @InjectMocks
    private TesouroDiretoSyncJob tesouroDiretoSyncJob;

    @Test
    @DisplayName("sincronizarTesouroDireto - deve delegar para o service de sincronização")
    void sincronizarTesouroDireto_deveDelegarParaOServiceDeSincronizacao() {
        tesouroDiretoSyncJob.sincronizarTesouroDireto();

        verify(tesouroDiretoSincronizacaoService).sincronizar();
    }

    @Test
    @DisplayName("sincronizarNaSubida - deve delegar para o service de sincronização assim que a aplicação sobe")
    void sincronizarNaSubida_deveDelegarParaOServiceDeSincronizacao() {
        tesouroDiretoSyncJob.sincronizarNaSubida();

        verify(tesouroDiretoSincronizacaoService).sincronizar();
    }
}