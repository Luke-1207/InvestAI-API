package com.investai.api.infra.bcb;

import com.investai.api.infra.bcb.dto.BcbSerieDTO;
import com.investai.api.infra.exception.BcbIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class BcbClientImplTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private BcbClientImpl bcbClientImpl;

    @BeforeEach
    void setUp() {
        bcbClientImpl = new BcbClientImpl(restClient);
        ReflectionTestUtils.setField(bcbClientImpl, "baseUrl", "https://api.bcb.gov.br");
    }

    @Test
    @DisplayName("obterSelicAtual - deve retornar o valor mais recente da série 432")
    void obterSelicAtual_deveRetornarValorMaisRecente() {
        mockarResposta(List.of(criarSerie("18/08/2026", "14.25")));

        BigDecimal resultado = bcbClientImpl.obterSelicAtual();

        assertThat(resultado).isEqualByComparingTo("14.25");
    }

    @Test
    @DisplayName("obterIpcaAcumulado12Meses - deve retornar o valor mais recente da série 13522")
    void obterIpcaAcumulado12Meses_deveRetornarValorMaisRecente() {
        mockarResposta(List.of(criarSerie("01/08/2026", "4.83")));

        BigDecimal resultado = bcbClientImpl.obterIpcaAcumulado12Meses();

        assertThat(resultado).isEqualByComparingTo("4.83");
    }

    @Test
    @DisplayName("obterSelicAtual - deve lançar exceção quando resposta vem vazia")
    void obterSelicAtual_deveLancarExcecaoQuandoRespostaVazia() {
        mockarResposta(List.of());

        assertThatThrownBy(() -> bcbClientImpl.obterSelicAtual())
                .isInstanceOf(BcbIndisponivelException.class);
    }

    @Test
    @DisplayName("obterSelicAtual - deve lançar exceção quando ocorre falha de rede")
    void obterSelicAtual_deveLancarExcecaoQuandoFalhaDeRede() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> bcbClientImpl.obterSelicAtual())
                .isInstanceOf(BcbIndisponivelException.class);
    }

    private void mockarResposta(List<BcbSerieDTO> resposta) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(resposta);
    }

    private BcbSerieDTO criarSerie(String data, String valor) {
        BcbSerieDTO serie = new BcbSerieDTO();
        serie.setData(data);
        serie.setValor(valor);
        return serie;
    }
}