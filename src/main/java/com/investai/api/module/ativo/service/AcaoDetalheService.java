package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.*;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.repository.AcaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AcaoDetalheService {

    private final AcaoRepository acaoRepository;
    private final CotacaoService cotacaoService;
    private final HistoricoService historicoService;

    public AcaoDetalheResponseDTO obterDetalhe(String codigo, String periodoGrafico) {
        String ticker = codigo.toUpperCase().trim();
        String periodo = (periodoGrafico == null || periodoGrafico.isBlank())
                ? PeriodoHistorico.UM_ANO.getCodigo()
                : periodoGrafico;

        Acao acao = acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Ativo não cadastrado ou inativo: " + ticker));

        AcaoDetalheResponseDTO.AcaoDetalheResponseDTOBuilder builder = AcaoDetalheResponseDTO.builder()
                .id(acao.getId())
                .codigo(acao.getCodigo())
                .nome(acao.getNome())
                .tipo(acao.getTipo())
                .setor(acao.getSetor())
                .ativo(acao.isAtivo())
                .precoLucro(null)
                .glossario(GlossarioAtivo.TERMOS);

        preencherCotacao(builder, ticker);
        preencherHistoricoEFaixa52Semanas(builder, ticker, periodo);

        return builder.build();
    }

    private void preencherCotacao(AcaoDetalheResponseDTO.AcaoDetalheResponseDTOBuilder builder, String ticker) {
        try {
            CotacaoResponseDTO cotacao = cotacaoService.obterCotacao(ticker);
            builder.preco(cotacao.getPreco())
                    .variacaoPercentual(cotacao.getVariacaoPercentual())
                    .variacaoPreco(cotacao.getVariacaoPreco())
                    .dividendYield(cotacao.getDividendYield())
                    .precoValorPatrimonial(cotacao.getPrecoValorPatrimonial())
                    .volume(cotacao.getVolume())
                    .cotacaoAtualizadaEm(cotacao.getAtualizadoEm())
                    .fonteCotacao(cotacao.getFonte())
                    .cotacaoDisponivel(true);
        } catch (ResourceNotFoundException e) {
            builder.cotacaoDisponivel(false);
        }
    }

    private void preencherHistoricoEFaixa52Semanas(
            AcaoDetalheResponseDTO.AcaoDetalheResponseDTOBuilder builder, String ticker, String periodoGrafico
    ) {
        try {
            HistoricoPrecoResponseDTO historicoAno =
                    historicoService.obterHistorico(ticker, PeriodoHistorico.UM_ANO.getCodigo());

            calcularFaixa52Semanas(historicoAno.getPontos(), builder);

            HistoricoPrecoResponseDTO historicoGrafico =
                    PeriodoHistorico.UM_ANO.getCodigo().equalsIgnoreCase(periodoGrafico)
                            ? historicoAno
                            : historicoService.obterHistorico(ticker, periodoGrafico);

            builder.periodoGrafico(historicoGrafico.getPeriodo())
                    .pontosGrafico(historicoGrafico.getPontos());

        } catch (ResourceNotFoundException e) {
            builder.periodoGrafico(periodoGrafico)
                    .pontosGrafico(List.of());
        }
    }

    private void calcularFaixa52Semanas(
            List<PontoHistoricoDTO> pontos, AcaoDetalheResponseDTO.AcaoDetalheResponseDTOBuilder builder
    ) {
        if (pontos == null || pontos.isEmpty()) {
            return;
        }

        BigDecimal minimo = pontos.stream()
                .map(PontoHistoricoDTO::getMinima)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal maximo = pontos.stream()
                .map(PontoHistoricoDTO::getMaxima)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);

        builder.minimo52Semanas(minimo)
                .maximo52Semanas(maximo);
    }
}
