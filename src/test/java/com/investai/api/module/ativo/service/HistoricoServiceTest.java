package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.hgbrasil.HgBrasilClient;
import com.investai.api.infra.hgbrasil.dto.HgBrasilHistoricalPointDTO;
import com.investai.api.module.ativo.dto.HistoricoPrecoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoServiceTest {

    @Mock
    private HgBrasilClient hgBrasilClient;

    @InjectMocks
    private HistoricoService historicoService;

    @Test
    @DisplayName("obterHistorico - deve retornar série mapeada com sucesso")
    void obterHistorico_deveRetornarSerieMapeadaComSucesso() {
        when(hgBrasilClient.obterHistorico("TAEE3", 30)).thenReturn(List.of(criarPonto()));

        HistoricoPrecoResponseDTO resultado = historicoService.obterHistorico("taee3", "1M");

        assertThat(resultado.getCodigo()).isEqualTo("TAEE3");
        assertThat(resultado.getPeriodo()).isEqualTo("1M");
        assertThat(resultado.getPontos()).hasSize(1);
        assertThat(resultado.getPontos().get(0).getFechamento()).isEqualByComparingTo(BigDecimal.valueOf(38.42));
    }

    @Test
    @DisplayName("obterHistorico - deve converter período 1S corretamente para 7 dias")
    void obterHistorico_deveConverterPeriodoUmaSemanaParaSeteDias() {
        when(hgBrasilClient.obterHistorico(eq("TAEE3"), eq(7))).thenReturn(List.of(criarPonto()));

        historicoService.obterHistorico("TAEE3", "1S");

        verify(hgBrasilClient).obterHistorico("TAEE3", 7);
    }

    @Test
    @DisplayName("obterHistorico - deve converter período 1A corretamente para 365 dias")
    void obterHistorico_deveConverterPeriodoUmAnoParaTrezentosESessentaECincoDias() {
        when(hgBrasilClient.obterHistorico(eq("TAEE3"), eq(365))).thenReturn(List.of(criarPonto()));

        historicoService.obterHistorico("TAEE3", "1A");

        verify(hgBrasilClient).obterHistorico("TAEE3", 365);
    }

    @Test
    @DisplayName("obterHistorico - deve normalizar ticker para maiúsculo")
    void obterHistorico_deveNormalizarTickerParaMaiusculo() {
        when(hgBrasilClient.obterHistorico(eq("PETR4"), anyInt())).thenReturn(List.of(criarPonto()));

        historicoService.obterHistorico("  petr4  ", "1M");

        verify(hgBrasilClient).obterHistorico("PETR4", 30);
    }

    @Test
    @DisplayName("obterHistorico - deve lançar BusinessException quando período inválido")
    void obterHistorico_deveLancarBusinessExceptionQuandoPeriodoInvalido() {
        assertThatThrownBy(() -> historicoService.obterHistorico("TAEE3", "2Y"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Período inválido");
    }

    @Test
    @DisplayName("obterHistorico - deve lançar ResourceNotFoundException quando ticker não encontrado")
    void obterHistorico_deveLancarResourceNotFoundQuandoTickerNaoEncontrado() {
        when(hgBrasilClient.obterHistorico("INEXISTENTE", 30))
                .thenThrow(new AtivoNaoEncontradoNaHgBrasilException("INEXISTENTE"));

        assertThatThrownBy(() -> historicoService.obterHistorico("inexistente", "1M"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("INEXISTENTE");
    }

    private HgBrasilHistoricalPointDTO criarPonto() {
        return HgBrasilHistoricalPointDTO.builder()
                .data(LocalDate.now())
                .abertura(BigDecimal.valueOf(38.0))
                .fechamento(BigDecimal.valueOf(38.42))
                .maxima(BigDecimal.valueOf(38.6))
                .minima(BigDecimal.valueOf(37.9))
                .volume(100_000L)
                .build();
    }
}