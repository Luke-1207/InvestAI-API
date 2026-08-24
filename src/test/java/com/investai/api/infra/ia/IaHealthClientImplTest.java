package com.investai.api.infra.ia;

import com.investai.api.infra.ia.dto.IaHealthResponseDTO;
import com.investai.api.infra.ia.dto.IaHealthStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class IaHealthClientImplTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private IaHealthClientImpl iaHealthClientImpl;

    @BeforeEach
    void setUp() {
        iaHealthClientImpl = new IaHealthClientImpl(restClient);
        ReflectionTestUtils.setField(iaHealthClientImpl, "baseUrl", "http://localhost:8000");
    }

    @Test
    @DisplayName("verificarStatus - deve retornar disponivel=true quando status é ok")
    void verificarStatus_deveRetornarDisponivelTrueQuandoStatusOk() {
        IaHealthResponseDTO response = new IaHealthResponseDTO();
        response.setStatus("ok");
        response.setRabbitmq(true);
        mockarResposta(response);

        IaHealthStatusDTO resultado = iaHealthClientImpl.verificarStatus();

        assertThat(resultado.isDisponivel()).isTrue();
        assertThat(resultado.getRabbitmqConectado()).isTrue();
    }

    @Test
    @DisplayName("verificarStatus - deve retornar disponivel=false quando status é diferente de ok")
    void verificarStatus_deveRetornarDisponivelFalseQuandoStatusDiferenteDeOk() {
        IaHealthResponseDTO response = new IaHealthResponseDTO();
        response.setStatus("degraded");
        response.setRabbitmq(false);
        mockarResposta(response);

        IaHealthStatusDTO resultado = iaHealthClientImpl.verificarStatus();

        assertThat(resultado.isDisponivel()).isFalse();
    }

    @Test
    @DisplayName("verificarStatus - deve retornar disponivel=false quando resposta vem vazia")
    void verificarStatus_deveRetornarDisponivelFalseQuandoRespostaVazia() {
        mockarResposta(null);

        IaHealthStatusDTO resultado = iaHealthClientImpl.verificarStatus();

        assertThat(resultado.isDisponivel()).isFalse();
        assertThat(resultado.getRabbitmqConectado()).isNull();
    }

    @Test
    @DisplayName("verificarStatus - nunca deve lançar exceção quando ocorre falha de rede")
    void verificarStatus_nuncaDeveLancarExcecaoQuandoFalhaDeRede() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(IaHealthResponseDTO.class)).thenThrow(new RestClientException("conexão recusada"));

        assertThatCode(() -> iaHealthClientImpl.verificarStatus()).doesNotThrowAnyException();

        IaHealthStatusDTO resultado = iaHealthClientImpl.verificarStatus();
        assertThat(resultado.isDisponivel()).isFalse();
    }

    private void mockarResposta(IaHealthResponseDTO response) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(IaHealthResponseDTO.class)).thenReturn(response);
    }
}