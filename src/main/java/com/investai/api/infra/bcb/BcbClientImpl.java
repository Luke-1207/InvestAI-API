package com.investai.api.infra.bcb;

import com.investai.api.infra.bcb.dto.BcbSerieDTO;
import com.investai.api.infra.exception.BcbIndisponivelException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BcbClientImpl implements BcbClient {

    private static final int SERIE_META_SELIC = 432;
    private static final int SERIE_IPCA_ACUMULADO_12M = 13522;

    private final RestClient bcbRestClient;

    @Value("${bcb.base-url}")
    private String baseUrl;

    @Override
    public BigDecimal obterSelicAtual() {
        return obterUltimoValorDaSerie(SERIE_META_SELIC);
    }

    @Override
    public BigDecimal obterIpcaAcumulado12Meses() {
        return obterUltimoValorDaSerie(SERIE_IPCA_ACUMULADO_12M);
    }

    private BigDecimal obterUltimoValorDaSerie(int codigoSerie) {
        String url = String.format("%s/dados/serie/bcdata.sgs.%d/dados/ultimos/1?formato=json", baseUrl, codigoSerie);

        try {
            List<BcbSerieDTO> resposta = bcbRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BcbSerieDTO>>() {});

            if (resposta == null || resposta.isEmpty()) {
                throw new BcbIndisponivelException("Resposta vazia da API do Banco Central (série " + codigoSerie + ")");
            }

            return new BigDecimal(resposta.get(0).getValor());

        } catch (BcbIndisponivelException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Falha ao consultar API do Banco Central (série {}): {}", codigoSerie, e.getMessage());
            throw new BcbIndisponivelException("Serviço do Banco Central indisponível no momento");
        }
    }
}