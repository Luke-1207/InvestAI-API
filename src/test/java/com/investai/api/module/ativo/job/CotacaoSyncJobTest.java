package com.investai.api.module.ativo.job;

import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.ativo.service.CotacaoService;
import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.service.MonitorIntegracoesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CotacaoSyncJobTest {

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private CotacaoService cotacaoService;

    @Mock
    private MonitorIntegracoesService monitorIntegracoesService;

    @InjectMocks
    private CotacaoSyncJob cotacaoSyncJob;

    @Test
    @DisplayName("sincronizarCotacoes - deve atualizar o cache para cada ativo ativo cadastrado")
    void sincronizarCotacoes_deveAtualizarCacheParaCadaAtivoAtivo() {
        Acao taee3 = criarAcaoMock("TAEE3");
        Acao petr4 = criarAcaoMock("PETR4");

        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(taee3, petr4));

        cotacaoSyncJob.sincronizarCotacoes();

        verify(cotacaoService).atualizarCacheSilenciosamente("TAEE3");
        verify(cotacaoService).atualizarCacheSilenciosamente("PETR4");
    }

    @Test
    @DisplayName("sincronizarCotacoes - não deve chamar o service quando não há ativos cadastrados")
    void sincronizarCotacoes_naoDeveChamarServiceQuandoListaVazia() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(Collections.emptyList());

        cotacaoSyncJob.sincronizarCotacoes();

        verify(cotacaoService, never()).atualizarCacheSilenciosamente(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("sincronizarCotacoes - deve processar todos os ativos da lista, um a um")
    void sincronizarCotacoes_deveProcessarTodosOsAtivosDaLista() {
        List<Acao> ativos = List.of(
                criarAcaoMock("TAEE3"),
                criarAcaoMock("PETR4"),
                criarAcaoMock("VALE3")
        );

        when(acaoRepository.findByAtivoTrue()).thenReturn(ativos);

        cotacaoSyncJob.sincronizarCotacoes();

        ativos.forEach(acao ->
                verify(cotacaoService).atualizarCacheSilenciosamente(acao.getCodigo())
        );
    }

    private Acao criarAcaoMock(String codigo) {
        return Acao.builder()
                .id(UUID.randomUUID())
                .codigo(codigo)
                .nome(codigo + " Empresa Teste")
                .tipo(TipoAtivo.ACAO)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("sincronizarCotacoes - basta uma cotação atualizada para registrar a HG Brasil como funcionando")
    void sincronizarCotacoes_algumaAtualizada_deveRegistrarSucesso() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(criarAcaoMock("TAEE3"), criarAcaoMock("XXXX3")));
        when(cotacaoService.atualizarCacheSilenciosamente("TAEE3")).thenReturn(true);
        when(cotacaoService.atualizarCacheSilenciosamente("XXXX3")).thenReturn(false);

        cotacaoSyncJob.sincronizarCotacoes();

        verify(monitorIntegracoesService).registrarSucesso(Integracao.HG_BRASIL);
        verify(monitorIntegracoesService, never()).registrarFalha(any(), any());
    }

    @Test
    @DisplayName("sincronizarCotacoes - se nenhuma cotação atualizar, deve registrar falha da HG Brasil")
    void sincronizarCotacoes_nenhumaAtualizada_deveRegistrarFalha() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(criarAcaoMock("TAEE3")));
        when(cotacaoService.atualizarCacheSilenciosamente("TAEE3")).thenReturn(false);

        cotacaoSyncJob.sincronizarCotacoes();

        verify(monitorIntegracoesService).registrarFalha(Integracao.HG_BRASIL, "Nenhuma cotação pôde ser atualizada na HG Brasil");
    }

    @Test
    @DisplayName("sincronizarCotacoes - sem ativos cadastrados não deve registrar nada")
    void sincronizarCotacoes_semAtivos_naoDeveRegistrar() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(Collections.emptyList());

        cotacaoSyncJob.sincronizarCotacoes();

        verifyNoInteractions(monitorIntegracoesService);
    }
}
