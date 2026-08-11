package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.AtivoNaoEncontradoNaHgBrasilException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.hgbrasil.HgBrasilClient;
import com.investai.api.infra.hgbrasil.dto.HgBrasilHistoricalPointDTO;
import com.investai.api.module.ativo.dto.HistoricoPrecoResponseDTO;
import com.investai.api.module.ativo.dto.PeriodoHistorico;
import com.investai.api.module.ativo.dto.PontoHistoricoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricoService {

    private final HgBrasilClient hgBrasilClient;

    public HistoricoPrecoResponseDTO obterHistorico(String codigo, String periodoCodigo) {
        String ticker = codigo.toUpperCase().trim();
        PeriodoHistorico periodo = PeriodoHistorico.fromCodigo(periodoCodigo);

        try {
            List<HgBrasilHistoricalPointDTO> pontosBrutos =
                    hgBrasilClient.obterHistorico(ticker, periodo.getDiasAtras());
            return toResponseDTO(ticker, periodo, pontosBrutos);
        } catch (AtivoNaoEncontradoNaHgBrasilException e) {
            throw new ResourceNotFoundException("Nenhum histórico encontrado para o ticker " + ticker);
        }
    }

    private HistoricoPrecoResponseDTO toResponseDTO(
            String ticker, PeriodoHistorico periodo, List<HgBrasilHistoricalPointDTO> pontosBrutos
    ) {
        List<PontoHistoricoDTO> pontos = pontosBrutos.stream()
                .map(p -> PontoHistoricoDTO.builder()
                        .data(p.getData())
                        .abertura(p.getAbertura())
                        .fechamento(p.getFechamento())
                        .maxima(p.getMaxima())
                        .minima(p.getMinima())
                        .volume(p.getVolume())
                        .build())
                .toList();

        return HistoricoPrecoResponseDTO.builder()
                .codigo(ticker)
                .periodo(periodo.getCodigo())
                .pontos(pontos)
                .build();
    }
}