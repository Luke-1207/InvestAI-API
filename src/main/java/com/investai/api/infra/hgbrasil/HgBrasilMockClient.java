package com.investai.api.infra.hgbrasil;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.hgbrasil.dto.HgBrasilDividendsDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilFinancialsDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilHistoricalPointDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Simula a API da HG Brasil enquanto o plano pago (Member Premium) não é contratado.
 * Fixtures baseadas no schema real documentado em console.hgbrasil.com/documentation/finance.
 * Para tickers fora da lista de fixtures, gera dados sintéticos determinísticos
 * (mesma seed = mesmo resultado sempre, útil para testes e demonstração).
 */
@Service
@ConditionalOnProperty(name = "hgbrasil.mock-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class HgBrasilMockClient implements HgBrasilClient {

    private static final Map<String, HgBrasilStockDTO> FIXTURES = new HashMap<>();

    static {
        FIXTURES.put("TAEE3", criarFixture("TAEE3", "stock", "Taesa - Transmissão de Energia",
                "Energia Elétrica", 38.42, 1.25, 0.48, 15200.0, 24_300_000L, 6.8, 1.3));

        FIXTURES.put("PETR4", criarFixture("PETR4", "stock", "Petrobras",
                "Petróleo, Gás e Biocombustíveis", 38.90, -0.42, -0.16, 505_000.0, 45_000_000L, 14.2, 1.1));

        FIXTURES.put("ITSA4", criarFixture("ITSA4", "stock", "Itaúsa",
                "Bancos", 13.74, 0.66, 0.09, 154_638.0, 21_934_800L, 9.67, 1.67));

        FIXTURES.put("VALE3", criarFixture("VALE3", "stock", "Vale",
                "Mineração", 61.30, -0.85, -0.53, 285_000.0, 32_000_000L, 9.1, 1.85));

        FIXTURES.put("HGLG11", criarFixture("HGLG11", "fii", "FII HGLG Pax",
                "Imóveis Industriais e Logísticos", 148.15, 0.37, 0.55, 6_755.92, 111_347L, 8.96, 0.89));

        FIXTURES.put("MXRF11", criarFixture("MXRF11", "fii", "FII Maxi Renda",
                "Financeiro e Outros/Fundos Imobiliários", 10.42, -0.19, -0.02, 3_200.0, 890_000L, 11.2, 1.02));

        FIXTURES.put("BOVA11", criarFixture("BOVA11", "etf", "iShares Ibovespa Fundo de Índice",
                "Índice", 128.90, 0.12, 0.15, 0.0, 540_000L, 0.0, 0.0));
    }

    @Override
    public HgBrasilStockDTO obterCotacao(String ticker) {
        String tickerNormalizado = ticker.toUpperCase().trim();

        if ("INVALIDO".equals(tickerNormalizado) || tickerNormalizado.isBlank()) {
            throw new AtivoNaoEncontradoNaHgBrasilException(tickerNormalizado);
        }

        HgBrasilStockDTO fixture = FIXTURES.get(tickerNormalizado);
        if (fixture != null) {
            log.debug("[MOCK] Retornando fixture fixa para {}", tickerNormalizado);
            return fixture;
        }

        log.debug("[MOCK] Gerando cotação sintética determinística para {}", tickerNormalizado);
        return gerarFixtureDeterministica(tickerNormalizado);
    }

    @Override
    public List<HgBrasilHistoricalPointDTO> obterHistorico(String ticker, int diasAtras) {
        String tickerNormalizado = ticker.toUpperCase().trim();

        if ("INVALIDO".equals(tickerNormalizado) || tickerNormalizado.isBlank()) {
            throw new AtivoNaoEncontradoNaHgBrasilException(tickerNormalizado);
        }

        log.debug("[MOCK] Gerando série histórica sintética para {} ({} dias)", tickerNormalizado, diasAtras);
        return gerarSerieDeterministica(tickerNormalizado, diasAtras);
    }

    private List<HgBrasilHistoricalPointDTO> gerarSerieDeterministica(String ticker, int diasAtras) {
        Random random = new Random(ticker.hashCode());
        double preco = 10 + random.nextDouble() * 90;

        List<HgBrasilHistoricalPointDTO> pontos = new ArrayList<>();
        LocalDate dataInicial = LocalDate.now().minusDays(diasAtras);

        for (int i = 0; i <= diasAtras; i++) {
            double variacaoDia = (random.nextDouble() * 4) - 2;
            double abertura = preco;
            double fechamento = round2(preco * (1 + variacaoDia / 100));
            double maxima = round2(Math.max(abertura, fechamento) * (1 + random.nextDouble() * 0.01));
            double minima = round2(Math.min(abertura, fechamento) * (1 - random.nextDouble() * 0.01));
            long volume = 50_000L + random.nextInt(500_000);

            pontos.add(HgBrasilHistoricalPointDTO.builder()
                    .data(dataInicial.plusDays(i))
                    .abertura(BigDecimal.valueOf(round2(abertura)))
                    .fechamento(BigDecimal.valueOf(fechamento))
                    .maxima(BigDecimal.valueOf(maxima))
                    .minima(BigDecimal.valueOf(minima))
                    .volume(volume)
                    .build());

            preco = fechamento;
        }

        return pontos;
    }

    private HgBrasilStockDTO gerarFixtureDeterministica(String ticker) {
        Random random = new Random(ticker.hashCode());

        double preco = 10 + random.nextDouble() * 90;
        double variacaoPercentual = round2((random.nextDouble() * 10) - 5);
        double variacaoPreco = round2(preco * variacaoPercentual / 100);
        double dy = round2(random.nextDouble() * 12);
        double pvp = round2(0.5 + random.nextDouble() * 2);

        return criarFixture(ticker, "stock", ticker + " (simulado)", "Diversos",
                round2(preco), variacaoPercentual, variacaoPreco, 0.0, 100_000L, dy, pvp);
    }

    private static double round2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static HgBrasilStockDTO criarFixture(String symbol, String kind, String name, String sector,
                                                 double price, double changePercent, double changePrice, double marketCap, long volume,
                                                 double dividendYield, double priceToBookRatio) {

        HgBrasilDividendsDTO dividends = new HgBrasilDividendsDTO();
        dividends.setYield12m(dividendYield);

        HgBrasilFinancialsDTO financials = new HgBrasilFinancialsDTO();
        financials.setPriceToBookRatio(priceToBookRatio);
        financials.setDividends(dividends);

        HgBrasilStockDTO dto = new HgBrasilStockDTO();
        dto.setSymbol(symbol);
        dto.setKind(kind);
        dto.setName(name);
        dto.setSector(sector);
        dto.setPrice(price);
        dto.setChangePercent(changePercent);
        dto.setChangePrice(changePrice);
        dto.setMarketCap(marketCap);
        dto.setVolume(volume);
        dto.setFinancials(financials);
        dto.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return dto;
    }
}
