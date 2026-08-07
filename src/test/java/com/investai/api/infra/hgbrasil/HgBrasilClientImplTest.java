package com.investai.api.infra.hgbrasil;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.exception.HgBrasilIndisponivelException;
import com.investai.api.infra.hgbrasil.dto.HgBrasilDividendsDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilFinancialsDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockPriceResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
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