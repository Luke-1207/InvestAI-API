package com.investai.api.module.ativo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.exception.HgBrasilIndisponivelException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.hgbrasil.HgBrasilClient;
import com.investai.api.infra.hgbrasil.dto.HgBrasilDividendsDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilFinancialsDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockDTO;
import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CotacaoServiceTest {

    @Mock
    private HgBrasilClient hgBrasilClient;

    @InjectMocks
    private CotacaoService cotacaoService;

    @Mock
    private Cache<String, CotacaoResponseDTO> cotacaoCaffeineCache;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cotacaoService, "mockEnabled", true);
    }

    @Test
    @DisplayName("obterCotacao - deve mapear dados da HG Brasil para o DTO interno")
    void obterCotacao_deveMapearDadosCorretamente() {
        when(hgBrasilClient.obterCotacao("TAEE3")).thenReturn(criarStockDTO());

        CotacaoResponseDTO resultado = cotacaoService.obterCotacao("taee3");

        assertThat(resultado.getCodigo()).isEqualTo("TAEE3");
        assertThat(resultado.getNome()).isEqualTo("Taesa");
        assertThat(resultado.getSetor()).isEqualTo("Energia Elétrica");
        assertThat(resultado.getPreco()).isEqualByComparingTo(BigDecimal.valueOf(38.42));
        assertThat(resultado.getVariacaoPercentual()).isEqualByComparingTo(BigDecimal.valueOf(1.25));
        assertThat(resultado.getDividendYield()).isEqualByComparingTo(BigDecimal.valueOf(6.8));
        assertThat(resultado.getPrecoValorPatrimonial()).isEqualByComparingTo(BigDecimal.valueOf(1.3));
        assertThat(resultado.getFonte()).isEqualTo("MOCK");
    }

    @Test
    @DisplayName("obterCotacao - deve indicar fonte HG_BRASIL quando mock desabilitado")
    void obterCotacao_deveIndicarFonteHgBrasilQuandoMockDesabilitado() {
        ReflectionTestUtils.setField(cotacaoService, "mockEnabled", false);
        when(hgBrasilClient.obterCotacao("TAEE3")).thenReturn(criarStockDTO());

        CotacaoResponseDTO resultado = cotacaoService.obterCotacao("TAEE3");

        assertThat(resultado.getFonte()).isEqualTo("HG_BRASIL");
    }

    @Test
    @DisplayName("obterCotacao - deve converter ticker para maiúsculo antes de consultar o client")
    void obterCotacao_deveConverterTickerParaMaiusculo() {
        when(hgBrasilClient.obterCotacao("PETR4")).thenReturn(criarStockDTO());

        cotacaoService.obterCotacao("  petr4  ");

        verify(hgBrasilClient).obterCotacao("PETR4");
    }

    @Test
    @DisplayName("obterCotacao - deve lançar ResourceNotFoundException quando ticker não encontrado")
    void obterCotacao_deveLancarResourceNotFoundQuandoTickerNaoEncontrado() {
        when(hgBrasilClient.obterCotacao("INEXISTENTE"))
                .thenThrow(new AtivoNaoEncontradoNaHgBrasilException("INEXISTENTE"));

        assertThatThrownBy(() -> cotacaoService.obterCotacao("inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("INEXISTENTE");
    }

    @Test
    @DisplayName("obterCotacao - deve lidar com financials nulo sem quebrar")
    void obterCotacao_deveLidarComFinancialsNulo() {
        HgBrasilStockDTO dados = criarStockDTO();
        dados.setFinancials(null);

        when(hgBrasilClient.obterCotacao("TAEE3")).thenReturn(dados);

        CotacaoResponseDTO resultado = cotacaoService.obterCotacao("TAEE3");

        assertThat(resultado.getDividendYield()).isNull();
        assertThat(resultado.getPrecoValorPatrimonial()).isNull();
    }

    @Test
    @DisplayName("obterCotacao - deve retornar do cache sem consultar o client quando disponível")
    void obterCotacao_deveRetornarDoCacheQuandoDisponivel() {
        CotacaoResponseDTO emCache = CotacaoResponseDTO.builder()
                .codigo("TAEE3")
                .preco(BigDecimal.valueOf(38.42))
                .fonte("MOCK")
                .build();

        when(cotacaoCaffeineCache.getIfPresent("TAEE3")).thenReturn(emCache);

        CotacaoResponseDTO resultado = cotacaoService.obterCotacao("taee3");

        assertThat(resultado).isEqualTo(emCache);
        verify(hgBrasilClient, never()).obterCotacao(anyString());
    }

    @Test
    @DisplayName("obterCotacao - deve buscar na fonte e aquecer o cache quando houver cache miss")
    void obterCotacao_deveBuscarNaFonteEAquecerCacheQuandoCacheMiss() {
        when(cotacaoCaffeineCache.getIfPresent("TAEE3")).thenReturn(null);
        when(hgBrasilClient.obterCotacao("TAEE3")).thenReturn(criarStockDTO());

        cotacaoService.obterCotacao("TAEE3");

        ArgumentCaptor<CotacaoResponseDTO> captor = ArgumentCaptor.forClass(CotacaoResponseDTO.class);
        verify(cotacaoCaffeineCache).put(eq("TAEE3"), captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("TAEE3");
    }

    @Test
    @DisplayName("atualizarCacheSilenciosamente - deve buscar e atualizar o cache com sucesso")
    void atualizarCacheSilenciosamente_deveAtualizarCacheComSucesso() {
        when(hgBrasilClient.obterCotacao("TAEE3")).thenReturn(criarStockDTO());

        cotacaoService.atualizarCacheSilenciosamente("taee3");

        verify(cotacaoCaffeineCache).put(eq("TAEE3"), any(CotacaoResponseDTO.class));
    }

    @Test
    @DisplayName("atualizarCacheSilenciosamente - não deve propagar exceção quando ticker não encontrado")
    void atualizarCacheSilenciosamente_naoDevePropagarExcecaoQuandoNaoEncontrado() {
        when(hgBrasilClient.obterCotacao("INEXISTENTE"))
                .thenThrow(new AtivoNaoEncontradoNaHgBrasilException("INEXISTENTE"));

        cotacaoService.atualizarCacheSilenciosamente("INEXISTENTE");

        verify(cotacaoCaffeineCache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("atualizarCacheSilenciosamente - não deve propagar exceção quando ocorre erro inesperado")
    void atualizarCacheSilenciosamente_naoDevePropagarExcecaoQuandoErroInesperado() {
        when(hgBrasilClient.obterCotacao("TAEE3"))
                .thenThrow(new HgBrasilIndisponivelException("Serviço indisponível"));

        cotacaoService.atualizarCacheSilenciosamente("TAEE3");

        verify(cotacaoCaffeineCache, never()).put(anyString(), any());
    }

    private HgBrasilStockDTO criarStockDTO() {
        HgBrasilDividendsDTO dividends = new HgBrasilDividendsDTO();
        dividends.setYield12m(6.8);

        HgBrasilFinancialsDTO financials = new HgBrasilFinancialsDTO();
        financials.setPriceToBookRatio(1.3);
        financials.setDividends(dividends);

        HgBrasilStockDTO dto = new HgBrasilStockDTO();
        dto.setSymbol("TAEE3");
        dto.setName("Taesa");
        dto.setSector("Energia Elétrica");
        dto.setPrice(38.42);
        dto.setChangePercent(1.25);
        dto.setChangePrice(0.48);
        dto.setVolume(24_300_000L);
        dto.setFinancials(financials);
        dto.setUpdatedAt("2026-08-06 14:32:00");
        return dto;
    }
}