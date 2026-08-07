package com.investai.api.infra.hgbrasil;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}