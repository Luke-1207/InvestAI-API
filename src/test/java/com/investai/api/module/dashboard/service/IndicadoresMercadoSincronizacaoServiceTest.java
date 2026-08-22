package com.investai.api.module.dashboard.service;

import com.investai.api.infra.bcb.BcbClient;
import com.investai.api.infra.exception.BcbIndisponivelException;
import com.investai.api.infra.exception.HgBrasilIndisponivelException;
import com.investai.api.infra.hgbrasil.HgBrasilClient;
import com.investai.api.infra.hgbrasil.dto.IndicadoresMercadoExternoDTO;
import com.investai.api.module.dashboard.dto.IndicadoresMercadoResponseDTO;
import com.investai.api.module.dashboard.entity.IndicadoresMercado;
import com.investai.api.module.dashboard.repository.IndicadoresMercadoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    }

    @Test
    @DisplayName("sincronizarSelicIpca - não deve gravar nada quando o Banco Central está indisponível")
    void sincronizarSelicIpca_naoDeveGravarNadaQuandoBcbIndisponivel() {
        when(bcbClient.obterSelicAtual()).thenThrow(new BcbIndisponivelException("BCB fora do ar"));

        indicadoresMercadoSincronizacaoService.sincronizarSelicIpca();

        verify(indicadoresMercadoRepository, never()).save(any());
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
    }
}