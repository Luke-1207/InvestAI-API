package com.investai.api.infra.hgbrasil;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.exception.HgBrasilIndisponivelException;
import com.investai.api.infra.hgbrasil.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

    @Override
    public List<HgBrasilHistoricalPointDTO> obterHistorico(String ticker, int diasAtras) {
        String url = String.format("%s/v2/finance/historical?key=%s&symbol=%s&days_ago=%d&sample_by=1d",
                baseUrl, apiKey, ticker.toLowerCase(), diasAtras);

        try {
            HgBrasilHistoricalResponseDTO response = hgBrasilRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(HgBrasilHistoricalResponseDTO.class);

            if (response == null || response.getResults() == null) {
                throw new HgBrasilIndisponivelException("Resposta vazia da HG Brasil");
            }

            Map<String, HgBrasilCandleDTO> serieDoTicker = response.getResults().get(ticker.toUpperCase());

            if (serieDoTicker == null || serieDoTicker.isEmpty()) {
                throw new AtivoNaoEncontradoNaHgBrasilException(ticker);
            }

            return serieDoTicker.entrySet().stream()
                    .map(entry -> toHistoricalPoint(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(HgBrasilHistoricalPointDTO::getData))
                    .toList();

        } catch (AtivoNaoEncontradoNaHgBrasilException | HgBrasilIndisponivelException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Falha ao consultar histórico da HG Brasil para o ticker {}: {}", ticker, e.getMessage());
            throw new HgBrasilIndisponivelException("Serviço de histórico indisponível no momento");
        }
    }

    private HgBrasilHistoricalPointDTO toHistoricalPoint(String timestampIso, HgBrasilCandleDTO candle) {
        LocalDate data = OffsetDateTime.parse(timestampIso).toLocalDate();
        return HgBrasilHistoricalPointDTO.builder()
                .data(data)
                .abertura(toBigDecimal(candle.getOpen()))
                .fechamento(toBigDecimal(candle.getClose()))
                .maxima(toBigDecimal(candle.getHigh()))
                .minima(toBigDecimal(candle.getLow()))
                .volume(candle.getVolume())
                .build();
    }

    private BigDecimal toBigDecimal(Double valor) {
        return valor != null ? BigDecimal.valueOf(valor) : null;
    }
}
