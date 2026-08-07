package com.investai.api.infra.hgbrasil;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.exception.HgBrasilIndisponivelException;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockPriceResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@ConditionalOnProperty(name = "hgbrasil.mock-enabled", havingValue = "false")
@RequiredArgsConstructor
@Slf4j
public class HgBrasilClientImpl implements HgBrasilClient {

    private final RestClient hgBrasilRestClient;

    @Value("${hgbrasil.base-url}")
    private String baseUrl;

    @Value("${hgbrasil.api-key}")
    private String apiKey;

    @Override
    public HgBrasilStockDTO obterCotacao(String ticker) {
        String url = String.format("%s/finance/stock_price?key=%s&symbol=%s",
                baseUrl, apiKey, ticker.toLowerCase());

        try {
            HgBrasilStockPriceResponseDTO response = hgBrasilRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(HgBrasilStockPriceResponseDTO.class);

            if (response == null || response.getResults() == null) {
                throw new HgBrasilIndisponivelException("Resposta vazia da HG Brasil");
            }

            HgBrasilStockDTO resultado = response.getResults().get(ticker.toUpperCase());

            if (resultado == null || Boolean.TRUE.equals(resultado.getError())) {
                throw new AtivoNaoEncontradoNaHgBrasilException(ticker);
            }

            return resultado;

        } catch (AtivoNaoEncontradoNaHgBrasilException | HgBrasilIndisponivelException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Falha ao consultar HG Brasil para o ticker {}: {}", ticker, e.getMessage());
            throw new HgBrasilIndisponivelException("Serviço de cotações indisponível no momento");
        }
    }
}
