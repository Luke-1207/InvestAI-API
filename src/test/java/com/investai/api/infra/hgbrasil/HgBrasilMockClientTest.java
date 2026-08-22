package com.investai.api.infra.hgbrasil;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.hgbrasil.dto.HgBrasilHistoricalPointDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockDTO;
import com.investai.api.infra.hgbrasil.dto.IndicadoresMercadoExternoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HgBrasilMockClientTest {

    private final HgBrasilMockClient hgBrasilMockClient = new HgBrasilMockClient();

    @Test
    @DisplayName("obterCotacao - deve retornar fixture fixa para ticker conhecido")
    void obterCotacao_deveRetornarFixtureFixaParaTickerConhecido() {
        HgBrasilStockDTO resultado = hgBrasilMockClient.obterCotacao("TAEE3");

        assertThat(resultado.getSymbol()).isEqualTo("TAEE3");
        assertThat(resultado.getName()).isEqualTo("Taesa - Transmissão de Energia");
        assertThat(resultado.getPrice()).isEqualTo(38.42);
        assertThat(resultado.getFinancials().getDividends().getYield12m()).isEqualTo(6.8);
    }

    @Test
    @DisplayName("obterCotacao - deve normalizar ticker em minúsculo para maiúsculo")
    void obterCotacao_deveNormalizarTickerParaMaiusculo() {
        HgBrasilStockDTO resultado = hgBrasilMockClient.obterCotacao("taee3");

        assertThat(resultado.getSymbol()).isEqualTo("TAEE3");
    }

    @Test
    @DisplayName("obterCotacao - deve lançar exceção para o ticker mágico INVALIDO")
    void obterCotacao_deveLancarExcecaoParaTickerInvalido() {
        assertThatThrownBy(() -> hgBrasilMockClient.obterCotacao("INVALIDO"))
                .isInstanceOf(AtivoNaoEncontradoNaHgBrasilException.class);
    }

    @Test
    @DisplayName("obterCotacao - deve lançar exceção quando ticker vazio")
    void obterCotacao_deveLancarExcecaoQuandoTickerVazio() {
        assertThatThrownBy(() -> hgBrasilMockClient.obterCotacao(""))
                .isInstanceOf(AtivoNaoEncontradoNaHgBrasilException.class);
    }

    @Test
    @DisplayName("obterCotacao - deve gerar cotação sintética determinística para ticker desconhecido")
    void obterCotacao_deveGerarCotacaoSinteticaDeterministica() {
        HgBrasilStockDTO primeira = hgBrasilMockClient.obterCotacao("XPTO4");
        HgBrasilStockDTO segunda = hgBrasilMockClient.obterCotacao("XPTO4");

        assertThat(primeira.getPrice()).isEqualTo(segunda.getPrice());
        assertThat(primeira.getChangePercent()).isEqualTo(segunda.getChangePercent());
        assertThat(primeira.getFinancials().getDividends().getYield12m())
                .isEqualTo(segunda.getFinancials().getDividends().getYield12m());
    }

    @Test
    @DisplayName("obterCotacao - tickers sintéticos diferentes devem gerar valores diferentes")
    void obterCotacao_tickersDiferentesDevemGerarValoresDiferentes() {
        HgBrasilStockDTO ticker1 = hgBrasilMockClient.obterCotacao("AAAA3");
        HgBrasilStockDTO ticker2 = hgBrasilMockClient.obterCotacao("ZZZZ4");

        assertThat(ticker1.getPrice()).isNotEqualTo(ticker2.getPrice());
    }

    @Test
    @DisplayName("obterHistorico - deve retornar série com diasAtras + 1 pontos")
    void obterHistorico_deveRetornarSerieComQuantidadeCorretaDePontos() {
        List<HgBrasilHistoricalPointDTO> resultado = hgBrasilMockClient.obterHistorico("TAEE3", 30);

        assertThat(resultado).hasSize(31);
    }

    @Test
    @DisplayName("obterHistorico - deve gerar pontos ordenados cronologicamente terminando hoje")
    void obterHistorico_deveGerarPontosOrdenadosTerminandoHoje() {
        List<HgBrasilHistoricalPointDTO> resultado = hgBrasilMockClient.obterHistorico("TAEE3", 7);

        assertThat(resultado.get(0).getData()).isEqualTo(LocalDate.now().minusDays(7));
        assertThat(resultado.get(resultado.size() - 1).getData()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("obterHistorico - deve ser determinístico para o mesmo ticker")
    void obterHistorico_deveSerDeterministicoParaMesmoTicker() {
        List<HgBrasilHistoricalPointDTO> primeira = hgBrasilMockClient.obterHistorico("XPTO4", 30);
        List<HgBrasilHistoricalPointDTO> segunda = hgBrasilMockClient.obterHistorico("XPTO4", 30);

        assertThat(primeira).usingRecursiveComparison().isEqualTo(segunda);
    }

    @Test
    @DisplayName("obterHistorico - tickers diferentes devem gerar séries diferentes")
    void obterHistorico_tickersDiferentesDevemGerarSeriesDiferentes() {
        List<HgBrasilHistoricalPointDTO> serie1 = hgBrasilMockClient.obterHistorico("AAAA3", 30);
        List<HgBrasilHistoricalPointDTO> serie2 = hgBrasilMockClient.obterHistorico("ZZZZ4", 30);

        assertThat(serie1.get(0).getFechamento()).isNotEqualTo(serie2.get(0).getFechamento());
    }

    @Test
    @DisplayName("obterHistorico - deve lançar exceção para o ticker mágico INVALIDO")
    void obterHistorico_deveLancarExcecaoParaTickerInvalido() {
        assertThatThrownBy(() -> hgBrasilMockClient.obterHistorico("INVALIDO", 30))
                .isInstanceOf(AtivoNaoEncontradoNaHgBrasilException.class);
    }

    @Test
    @DisplayName("obterHistorico - cada ponto deve ter máxima >= mínima e valores coerentes")
    void obterHistorico_cadaPontoDeveTerValoresCoerentes() {
        List<HgBrasilHistoricalPointDTO> resultado = hgBrasilMockClient.obterHistorico("TAEE3", 10);

        resultado.forEach(ponto -> {
            assertThat(ponto.getMaxima()).isGreaterThanOrEqualTo(ponto.getMinima());
            assertThat(ponto.getFechamento()).isPositive();
            assertThat(ponto.getVolume()).isPositive();
        });
    }

    @Test
    @DisplayName("obterIndicadoresMercado - deve retornar valores fixos de fixture")
    void obterIndicadoresMercado_deveRetornarValoresFixos() {
        IndicadoresMercadoExternoDTO resultado = hgBrasilMockClient.obterIndicadoresMercado();

        assertThat(resultado.getIbovespaPontos()).isEqualByComparingTo("134820.5");
        assertThat(resultado.getIbovespaVariacaoDia()).isEqualByComparingTo("-1.25");
        assertThat(resultado.getDolarValor()).isEqualByComparingTo("5.14");
        assertThat(resultado.getEuroValor()).isEqualByComparingTo("6.02");
    }
}