package com.investai.api.module.dashboard.service;

import com.investai.api.infra.bcb.BcbClient;
import com.investai.api.infra.exception.BcbIndisponivelException;
import com.investai.api.infra.exception.HgBrasilIndisponivelException;
import com.investai.api.infra.hgbrasil.HgBrasilClient;
import com.investai.api.infra.hgbrasil.dto.IndicadoresMercadoExternoDTO;
import com.investai.api.module.dashboard.dto.IndicadoresMercadoResponseDTO;
import com.investai.api.module.dashboard.entity.IndicadoresMercado;
import com.investai.api.module.dashboard.repository.IndicadoresMercadoRepository;
import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.service.MonitorIntegracoesService;
import com.investai.api.shared.event.IndicadoresMercadoAlteradosEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndicadoresMercadoSincronizacaoServiceTest {

    @Mock
    private IndicadoresMercadoRepository indicadoresMercadoRepository;

    @Mock
    private HgBrasilClient hgBrasilClient;

    @Mock
    private BcbClient bcbClient;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private MonitorIntegracoesService monitorIntegracoesService;

    @InjectMocks
    private IndicadoresMercadoSincronizacaoService indicadoresMercadoSincronizacaoService;

    @Test
    @DisplayName("sincronizarMercado - deve criar o snapshot quando não existe nenhum ainda")
    void sincronizarMercado_deveCriarSnapshotQuandoNaoExiste() {
        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(hgBrasilClient.obterIndicadoresMercado()).thenReturn(IndicadoresMercadoExternoDTO.builder()
                .ibovespaPontos(BigDecimal.valueOf(134820.5))
                .ibovespaVariacaoDia(BigDecimal.valueOf(-1.25))
                .dolarValor(BigDecimal.valueOf(5.14))
                .dolarVariacaoDia(BigDecimal.valueOf(-0.3))
                .euroValor(BigDecimal.valueOf(6.02))
                .euroVariacaoDia(BigDecimal.valueOf(0.12))
                .build());

        indicadoresMercadoSincronizacaoService.sincronizarMercado();

        ArgumentCaptor<IndicadoresMercado> captor = ArgumentCaptor.forClass(IndicadoresMercado.class);
        verify(indicadoresMercadoRepository).save(captor.capture());

        IndicadoresMercado salvo = captor.getValue();
        assertThat(salvo.getIbovespaPontos()).isEqualByComparingTo("134820.5");
        assertThat(salvo.getDolarValor()).isEqualByComparingTo("5.14");
        assertThat(salvo.getEuroValor()).isEqualByComparingTo("6.02");
        assertThat(salvo.getSincronizadoEm()).isNotNull();
        verify(monitorIntegracoesService).registrarSucesso(Integracao.HG_BRASIL);
    }

    @Test
    @DisplayName("sincronizarMercado - deve atualizar o snapshot existente sem criar um novo, preservando selic/ipca")
    void sincronizarMercado_deveAtualizarSnapshotExistente() {
        IndicadoresMercado existente = IndicadoresMercado.builder()
                .id(UUID.randomUUID())
                .ibovespaPontos(BigDecimal.valueOf(100000))
                .selicAtual(BigDecimal.valueOf(14.25))
                .build();

        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(existente));
        when(hgBrasilClient.obterIndicadoresMercado())
                .thenReturn(IndicadoresMercadoExternoDTO.builder().ibovespaPontos(BigDecimal.valueOf(134820.5)).build());

        indicadoresMercadoSincronizacaoService.sincronizarMercado();

        ArgumentCaptor<IndicadoresMercado> captor = ArgumentCaptor.forClass(IndicadoresMercado.class);
        verify(indicadoresMercadoRepository).save(captor.capture());

        IndicadoresMercado salvo = captor.getValue();
        assertThat(salvo.getId()).isEqualTo(existente.getId());
        assertThat(salvo.getIbovespaPontos()).isEqualByComparingTo("134820.5");
        assertThat(salvo.getSelicAtual()).isEqualByComparingTo("14.25");
    }

    @Test
    @DisplayName("sincronizarMercado - não deve gravar nada quando HG Brasil está indisponível")
    void sincronizarMercado_naoDeveGravarNadaQuandoHgBrasilIndisponivel() {
        when(hgBrasilClient.obterIndicadoresMercado())
                .thenThrow(new HgBrasilIndisponivelException("HG Brasil fora do ar"));

        indicadoresMercadoSincronizacaoService.sincronizarMercado();

        verify(indicadoresMercadoRepository, never()).save(any());
        verify(monitorIntegracoesService).registrarFalha(Integracao.HG_BRASIL, "HG Brasil fora do ar");
        verify(monitorIntegracoesService, never()).registrarSucesso(any());
    }

    @Test
    @DisplayName("sincronizarSelicIpca - deve atualizar selic e ipca com o timestamp próprio")
    void sincronizarSelicIpca_deveAtualizarSelicEIpca() {
        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(bcbClient.obterSelicAtual()).thenReturn(BigDecimal.valueOf(14.25));
        when(bcbClient.obterIpcaAcumulado12Meses()).thenReturn(BigDecimal.valueOf(4.83));
        when(indicadoresMercadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        indicadoresMercadoSincronizacaoService.sincronizarSelicIpca();

        ArgumentCaptor<IndicadoresMercado> captor = ArgumentCaptor.forClass(IndicadoresMercado.class);
        verify(indicadoresMercadoRepository).save(captor.capture());

        IndicadoresMercado salvo = captor.getValue();
        assertThat(salvo.getSelicAtual()).isEqualByComparingTo("14.25");
        assertThat(salvo.getIpcaAcumulado12m()).isEqualByComparingTo("4.83");
        assertThat(salvo.getSelicIpcaSincronizadoEm()).isNotNull();
        verify(monitorIntegracoesService).registrarSucesso(Integracao.BANCO_CENTRAL);
    }

    @Test
    @DisplayName("sincronizarSelicIpca - não deve gravar nada quando o Banco Central está indisponível")
    void sincronizarSelicIpca_naoDeveGravarNadaQuandoBcbIndisponivel() {
        when(bcbClient.obterSelicAtual()).thenThrow(new BcbIndisponivelException("BCB fora do ar"));

        indicadoresMercadoSincronizacaoService.sincronizarSelicIpca();

        verify(indicadoresMercadoRepository, never()).save(any());
        verify(monitorIntegracoesService).registrarFalha(Integracao.BANCO_CENTRAL, "BCB fora do ar");
    }

    @Test
    @DisplayName("atualizarSelicIpcaManualmente - deve atualizar e retornar o snapshot completo")
    void atualizarSelicIpcaManualmente_deveAtualizarERetornarSnapshot() {
        IndicadoresMercado existente = IndicadoresMercado.builder()
                .id(UUID.randomUUID())
                .ibovespaPontos(BigDecimal.valueOf(134820.5))
                .build();

        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(existente));
        when(indicadoresMercadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IndicadoresMercadoResponseDTO resultado = indicadoresMercadoSincronizacaoService
                .atualizarSelicIpcaManualmente(BigDecimal.valueOf(14.25), BigDecimal.valueOf(4.83));

        assertThat(resultado.getSelicAtual()).isEqualByComparingTo("14.25");
        assertThat(resultado.getIpcaAcumulado12m()).isEqualByComparingTo("4.83");
        assertThat(resultado.getIbovespaPontos()).isEqualByComparingTo("134820.5");
        assertThat(resultado.getSelicIpcaSincronizadoEm()).isNotNull();
        verify(applicationEventPublisher).publishEvent(any(IndicadoresMercadoAlteradosEvent.class));
        verifyNoInteractions(monitorIntegracoesService);
    }

    @Test
    @DisplayName("obterSnapshotAtual - deve retornar o snapshot mapeado quando existe")
    void obterSnapshotAtual_deveRetornarSnapshotMapeadoQuandoExiste() {
        IndicadoresMercado existente = IndicadoresMercado.builder()
                .id(UUID.randomUUID())
                .ibovespaPontos(BigDecimal.valueOf(134820.5))
                .selicAtual(BigDecimal.valueOf(14.25))
                .build();

        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(existente));

        IndicadoresMercadoResponseDTO resultado = indicadoresMercadoSincronizacaoService.obterSnapshotAtual();

        assertThat(resultado.getIbovespaPontos()).isEqualByComparingTo("134820.5");
        assertThat(resultado.getSelicAtual()).isEqualByComparingTo("14.25");
    }

    @Test
    @DisplayName("obterSnapshotAtual - deve retornar DTO com campos nulos quando nenhum snapshot existe ainda")
    void obterSnapshotAtual_deveRetornarDTOVazioQuandoNaoExisteSnapshot() {
        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        IndicadoresMercadoResponseDTO resultado = indicadoresMercadoSincronizacaoService.obterSnapshotAtual();

        assertThat(resultado).isNotNull();
        assertThat(resultado.getIbovespaPontos()).isNull();
    }

    @Test
    @DisplayName("sincronizarMercado - deve avisar o dashboard para descartar o cache depois de gravar")
    void sincronizarMercado_devePublicarEventoDeIndicadoresAlterados() {
        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(hgBrasilClient.obterIndicadoresMercado()).thenReturn(IndicadoresMercadoExternoDTO.builder().build());

        indicadoresMercadoSincronizacaoService.sincronizarMercado();

        verify(applicationEventPublisher).publishEvent(any(IndicadoresMercadoAlteradosEvent.class));
    }

    @Test
    @DisplayName("sincronizarSelicIpca - deve avisar o dashboard para descartar o cache depois de gravar")
    void sincronizarSelicIpca_devePublicarEventoDeIndicadoresAlterados() {
        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(bcbClient.obterSelicAtual()).thenReturn(BigDecimal.TEN);
        when(bcbClient.obterIpcaAcumulado12Meses()).thenReturn(BigDecimal.ONE);
        when(indicadoresMercadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        indicadoresMercadoSincronizacaoService.sincronizarSelicIpca();

        verify(applicationEventPublisher).publishEvent(any(IndicadoresMercadoAlteradosEvent.class));
    }

    @Test
    @DisplayName("sincronizarSelicIpca - falha no Banco Central não deve invalidar o cache")
    void sincronizarSelicIpca_falha_naoDevePublicarEvento() {
        when(bcbClient.obterSelicAtual()).thenThrow(new BcbIndisponivelException("timeout"));

        indicadoresMercadoSincronizacaoService.sincronizarSelicIpca();

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("selicIpcaDesatualizados - sem snapshot ou sem sincronização registrada deve pedir sincronização")
    void selicIpcaDesatualizados_semDados_deveSerTrue() {
        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        assertThat(indicadoresMercadoSincronizacaoService.selicIpcaDesatualizados()).isTrue();

        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(new IndicadoresMercado()));
        assertThat(indicadoresMercadoSincronizacaoService.selicIpcaDesatualizados()).isTrue();
    }

    @Test
    @DisplayName("selicIpcaDesatualizados - deve considerar desatualizado só depois de 24 horas")
    void selicIpcaDesatualizados_deveRespeitarAValidade() {
        IndicadoresMercado recente = new IndicadoresMercado();
        recente.setSelicIpcaSincronizadoEm(LocalDateTime.now().minusHours(2));
        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(recente));
        assertThat(indicadoresMercadoSincronizacaoService.selicIpcaDesatualizados()).isFalse();

        IndicadoresMercado antigo = new IndicadoresMercado();
        antigo.setSelicIpcaSincronizadoEm(LocalDateTime.now().minusHours(25));
        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(antigo));
        assertThat(indicadoresMercadoSincronizacaoService.selicIpcaDesatualizados()).isTrue();
    }
}
