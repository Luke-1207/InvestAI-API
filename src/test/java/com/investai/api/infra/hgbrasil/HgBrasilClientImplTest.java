package com.investai.api.infra.hgbrasil;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.exception.HgBrasilIndisponivelException;
import com.investai.api.infra.hgbrasil.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class HgBrasilClientImplTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private HgBrasilClientImpl hgBrasilClientImpl;

    @BeforeEach
    void setUp() {
        hgBrasilClientImpl = new HgBrasilClientImpl(restClient);
        ReflectionTestUtils.setField(hgBrasilClientImpl, "baseUrl", "https://api.hgbrasil.com");
        ReflectionTestUtils.setField(hgBrasilClientImpl, "apiKey", "chave-teste");
    }

    @Test
    @DisplayName("obterCotacao - deve retornar dados quando ticker existe e é válido")
    void obterCotacao_deveRetornarDadosQuandoTickerValido() {
        HgBrasilStockDTO stock = criarStockDTO("TAEE3", false);
        mockarCadeiaRestClient(criarResponse("TAEE3", stock));

        HgBrasilStockDTO resultado = hgBrasilClientImpl.obterCotacao("taee3");

        assertThat(resultado.getSymbol()).isEqualTo("TAEE3");
        assertThat(resultado.getPrice()).isEqualTo(38.42);
    }

    @Test
    @DisplayName("obterCotacao - deve lançar exceção quando ticker não está nos resultados")
    void obterCotacao_deveLancarExcecaoQuandoTickerAusenteNosResultados() {
        mockarCadeiaRestClient(criarResponse("OUTRO", criarStockDTO("OUTRO", false)));

        assertThatThrownBy(() -> hgBrasilClientImpl.obterCotacao("INEXISTENTE"))
                .isInstanceOf(AtivoNaoEncontradoNaHgBrasilException.class);
    }

    @Test
    @DisplayName("obterCotacao - deve lançar exceção quando a HG Brasil retorna flag de erro")
    void obterCotacao_deveLancarExcecaoQuandoFlagDeErro() {
        HgBrasilStockDTO stockComErro = criarStockDTO("EMBR3", true);
        mockarCadeiaRestClient(criarResponse("EMBR3", stockComErro));

        assertThatThrownBy(() -> hgBrasilClientImpl.obterCotacao("EMBR3"))
                .isInstanceOf(AtivoNaoEncontradoNaHgBrasilException.class);
    }

    @Test
    @DisplayName("obterCotacao - deve lançar exceção quando resposta vem vazia")
    void obterCotacao_deveLancarExcecaoQuandoRespostaVazia() {
        mockarCadeiaRestClient(null);

        assertThatThrownBy(() -> hgBrasilClientImpl.obterCotacao("TAEE3"))
                .isInstanceOf(HgBrasilIndisponivelException.class);
    }

    @Test
    @DisplayName("obterCotacao - deve lançar exceção quando ocorre falha de rede/HTTP")
    void obterCotacao_deveLancarExcecaoQuandoFalhaDeRede() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(HgBrasilStockPriceResponseDTO.class))
                .thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> hgBrasilClientImpl.obterCotacao("TAEE3"))
                .isInstanceOf(HgBrasilIndisponivelException.class);
    }

    @Test
    @DisplayName("obterHistorico - deve retornar série ordenada por data quando ticker existe")
    void obterHistorico_deveRetornarSerieOrdenadaQuandoTickerExiste() {
        Map<String, HgBrasilCandleDTO> serie = new LinkedHashMap<>();
        serie.put("2026-05-15T13:00:00.000000Z", criarCandle(39.0, 39.5, 39.8, 38.9, 100_000L));
        serie.put("2026-05-14T13:00:00.000000Z", criarCandle(38.0, 38.4, 38.6, 37.9, 90_000L));

        mockarCadeiaRestClientHistorico(criarHistoricalResponse("TAEE3", serie));

        List<HgBrasilHistoricalPointDTO> resultado = hgBrasilClientImpl.obterHistorico("taee3", 30);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getData()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(resultado.get(1).getData()).isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(resultado.get(0).getFechamento()).isEqualByComparingTo(BigDecimal.valueOf(38.4));
    }

    @Test
    @DisplayName("obterHistorico - deve lançar exceção quando ticker não está nos resultados")
    void obterHistorico_deveLancarExcecaoQuandoTickerAusente() {
        mockarCadeiaRestClientHistorico(criarHistoricalResponse("OUTRO", new LinkedHashMap<>()));

        assertThatThrownBy(() -> hgBrasilClientImpl.obterHistorico("INEXISTENTE", 30))
                .isInstanceOf(AtivoNaoEncontradoNaHgBrasilException.class);
    }

    @Test
    @DisplayName("obterHistorico - deve lançar exceção quando série do ticker vem vazia")
    void obterHistorico_deveLancarExcecaoQuandoSerieVazia() {
        Map<String, Map<String, HgBrasilCandleDTO>> results = new HashMap<>();
        results.put("TAEE3", new LinkedHashMap<>());

        HgBrasilHistoricalResponseDTO response = new HgBrasilHistoricalResponseDTO();
        response.setResults(results);

        mockarCadeiaRestClientHistorico(response);

        assertThatThrownBy(() -> hgBrasilClientImpl.obterHistorico("TAEE3", 30))
                .isInstanceOf(AtivoNaoEncontradoNaHgBrasilException.class);
    }

    @Test
    @DisplayName("obterHistorico - deve lançar exceção quando resposta vem vazia")
    void obterHistorico_deveLancarExcecaoQuandoRespostaVazia() {
        mockarCadeiaRestClientHistorico(null);

        assertThatThrownBy(() -> hgBrasilClientImpl.obterHistorico("TAEE3", 30))
                .isInstanceOf(HgBrasilIndisponivelException.class);
    }

    @Test
    @DisplayName("obterHistorico - deve lançar exceção quando ocorre falha de rede/HTTP")
    void obterHistorico_deveLancarExcecaoQuandoFalhaDeRede() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(HgBrasilHistoricalResponseDTO.class))
                .thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> hgBrasilClientImpl.obterHistorico("TAEE3", 30))
                .isInstanceOf(HgBrasilIndisponivelException.class);
    }

    private void mockarCadeiaRestClientHistorico(HgBrasilHistoricalResponseDTO response) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(HgBrasilHistoricalResponseDTO.class)).thenReturn(response);
    }

    private HgBrasilHistoricalResponseDTO criarHistoricalResponse(String ticker, Map<String, HgBrasilCandleDTO> serie) {
        Map<String, Map<String, HgBrasilCandleDTO>> results = new HashMap<>();
        results.put(ticker, serie);

        HgBrasilHistoricalResponseDTO response = new HgBrasilHistoricalResponseDTO();
        response.setResults(results);
        return response;
    }

    private HgBrasilCandleDTO criarCandle(double open, double close, double high, double low, long volume) {
        HgBrasilCandleDTO candle = new HgBrasilCandleDTO();
        candle.setOpen(open);
        candle.setClose(close);
        candle.setHigh(high);
        candle.setLow(low);
        candle.setVolume(volume);
        return candle;
    }

    private void mockarCadeiaRestClient(HgBrasilStockPriceResponseDTO response) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(HgBrasilStockPriceResponseDTO.class)).thenReturn(response);
    }

    private HgBrasilStockPriceResponseDTO criarResponse(String ticker, HgBrasilStockDTO stock) {
        Map<String, HgBrasilStockDTO> results = new HashMap<>();
        results.put(ticker, stock);

        HgBrasilStockPriceResponseDTO response = new HgBrasilStockPriceResponseDTO();
        response.setResults(results);
        return response;
    }

    private HgBrasilStockDTO criarStockDTO(String symbol, boolean erro) {
        HgBrasilDividendsDTO dividends = new HgBrasilDividendsDTO();
        dividends.setYield12m(6.8);

        HgBrasilFinancialsDTO financials = new HgBrasilFinancialsDTO();
        financials.setPriceToBookRatio(1.3);
        financials.setDividends(dividends);

        HgBrasilStockDTO dto = new HgBrasilStockDTO();
        dto.setSymbol(symbol);
        dto.setError(erro);
        dto.setPrice(38.42);
        dto.setChangePercent(1.25);
        dto.setFinancials(financials);
        return dto;
    }
}