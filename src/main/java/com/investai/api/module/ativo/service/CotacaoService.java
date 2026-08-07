package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.hgbrasil.HgBrasilClient;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockDTO;
import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CotacaoService {

    private final HgBrasilClient hgBrasilClient;

    @Value("${hgbrasil.mock-enabled:true}")
    private boolean mockEnabled;

    public CotacaoResponseDTO obterCotacao(String codigo) {
        String ticker = codigo.toUpperCase().trim();

        try {
            HgBrasilStockDTO dados = hgBrasilClient.obterCotacao(ticker);
            return toResponseDTO(dados);
        } catch (AtivoNaoEncontradoNaHgBrasilException e) {
            throw new ResourceNotFoundException("Nenhuma cotação encontrada para o ticker " + ticker);
        }
    }

    private CotacaoResponseDTO toResponseDTO(HgBrasilStockDTO dados) {
        Double dy = dados.getFinancials() != null && dados.getFinancials().getDividends() != null
                ? dados.getFinancials().getDividends().getYield12m()
                : null;

        Double pvp = dados.getFinancials() != null
                ? dados.getFinancials().getPriceToBookRatio()
                : null;

        return CotacaoResponseDTO.builder()
                .codigo(dados.getSymbol())
                .nome(dados.getName())
                .setor(dados.getSector())
                .preco(toBigDecimal(dados.getPrice()))
                .variacaoPercentual(toBigDecimal(dados.getChangePercent()))
                .variacaoPreco(toBigDecimal(dados.getChangePrice()))
                .dividendYield(toBigDecimal(dy))
                .precoValorPatrimonial(toBigDecimal(pvp))
                .volume(dados.getVolume())
                .atualizadoEm(parseDataAtualizacao(dados.getUpdatedAt()))
                .fonte(mockEnabled ? "MOCK" : "HG_BRASIL")
                .build();
    }

    private BigDecimal toBigDecimal(Double valor) {
        return valor != null ? BigDecimal.valueOf(valor) : null;
    }

    private LocalDateTime parseDataAtualizacao(String updatedAt) {
        if (updatedAt == null) {
            return null;
        }
        return LocalDateTime.parse(updatedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
