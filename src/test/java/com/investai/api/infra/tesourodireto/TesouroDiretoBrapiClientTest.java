package com.investai.api.infra.tesourodireto;

import com.investai.api.infra.exception.TesouroDiretoIndisponivelException;
import com.investai.api.infra.tesourodireto.dto.BrapiTreasuryDTO;
import com.investai.api.infra.tesourodireto.dto.BrapiTreasuryIndicatorsResponseDTO;
import com.investai.api.infra.tesourodireto.dto.TituloTesouroExternoDTO;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class TesouroDiretoBrapiClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private TesouroDiretoBrapiClient tesouroDiretoBrapiClient;

    @BeforeEach
    void setUp() {
        tesouroDiretoBrapiClient = new TesouroDiretoBrapiClient(restClient);
        ReflectionTestUtils.setField(tesouroDiretoBrapiClient, "baseUrl", "https://brapi.dev/api/v2/treasury");
    }

    @Test
    @DisplayName("buscarTitulosDisponiveis - deve mapear os títulos reais da brapi e completar com os de volume")
    void buscarTitulosDisponiveis_deveMapearTitulosReaisEComplementarComVolume() {
        BrapiTreasuryDTO selic = new BrapiTreasuryDTO();
        selic.setSymbol("tesouro-selic-01032031");
        selic.setBondType("Tesouro Selic");
        selic.setIndexer("selic");
        selic.setCouponType("zero");
        selic.setMaturityDate("2031-03-01");
        selic.setBuyRate(0.08);
        selic.setBuyPrice(18944.78);

        BrapiTreasuryDTO ipcaSemestral = new BrapiTreasuryDTO();
        ipcaSemestral.setSymbol("tesouro-ipca-com-juros-semestrais-15082060");
        ipcaSemestral.setBondType("Tesouro IPCA+ com Juros Semestrais");
        ipcaSemestral.setIndexer("ipca");
        ipcaSemestral.setCouponType("semestral");
        ipcaSemestral.setMaturityDate("2060-08-15");
        ipcaSemestral.setBuyRate(7.22);
        ipcaSemestral.setBuyPrice(4086.85);

        mockarCadeiaRestClient(criarResponse(List.of(selic, ipcaSemestral)));

        List<TituloTesouroExternoDTO> resultado = tesouroDiretoBrapiClient.buscarTitulosDisponiveis();

        // 2 reais da brapi + 4 de volume (TesouroDiretoFixturesVolume)
        assertThat(resultado).hasSize(6);

        TituloTesouroExternoDTO tituloSelic = resultado.get(0);
        assertThat(tituloSelic.getCodigo()).isEqualTo("tesouro-selic-01032031");
        assertThat(tituloSelic.getNome()).isEqualTo("Tesouro Selic 2031");
        assertThat(tituloSelic.getTipo()).isEqualTo("SELIC");
        assertThat(tituloSelic.getTaxaAnual()).isEqualByComparingTo(BigDecimal.valueOf(0.08));
        assertThat(tituloSelic.getPrecoMinimo()).isEqualByComparingTo("189.45"); // 1% de 18944.78
        assertThat(tituloSelic.getVencimento()).isEqualTo(LocalDate.of(2031, 3, 1));
        assertThat(tituloSelic.isPagaJurosSemestrais()).isFalse();

        TituloTesouroExternoDTO tituloIpca = resultado.get(1);
        assertThat(tituloIpca.getTipo()).isEqualTo("IPCA");
        assertThat(tituloIpca.isPagaJurosSemestrais()).isTrue();
        assertThat(tituloIpca.getNome()).isEqualTo("Tesouro IPCA+ com Juros Semestrais 2060");
    }

    @Test
    @DisplayName("buscarTitulosDisponiveis - deve lançar exceção quando a resposta vem vazia")
    void buscarTitulosDisponiveis_deveLancarExcecaoQuandoRespostaVazia() {
        mockarCadeiaRestClient(criarResponse(List.of()));

        assertThatThrownBy(() -> tesouroDiretoBrapiClient.buscarTitulosDisponiveis())
                .isInstanceOf(TesouroDiretoIndisponivelException.class);
    }

    @Test
    @DisplayName("buscarTitulosDisponiveis - deve lançar exceção quando ocorre falha de rede/HTTP")
    void buscarTitulosDisponiveis_deveLancarExcecaoQuandoFalhaDeRede() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(BrapiTreasuryIndicatorsResponseDTO.class))
                .thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> tesouroDiretoBrapiClient.buscarTitulosDisponiveis())
                .isInstanceOf(TesouroDiretoIndisponivelException.class);
    }

    private void mockarCadeiaRestClient(BrapiTreasuryIndicatorsResponseDTO response) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(BrapiTreasuryIndicatorsResponseDTO.class)).thenReturn(response);
    }

    private BrapiTreasuryIndicatorsResponseDTO criarResponse(List<BrapiTreasuryDTO> titulos) {
        BrapiTreasuryIndicatorsResponseDTO response = new BrapiTreasuryIndicatorsResponseDTO();
        response.setResults(titulos);
        return response;
    }
}