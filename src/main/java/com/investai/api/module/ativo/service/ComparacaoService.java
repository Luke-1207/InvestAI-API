package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.AcaoListagemResponseDTO;
import com.investai.api.module.ativo.dto.ComparacaoResponseDTO;
import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.repository.AcaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class ComparacaoService {

    private static final int MINIMO_ATIVOS = 2;
    private static final int MAXIMO_ATIVOS = 5;

    private final AcaoRepository acaoRepository;
    private final CotacaoService cotacaoService;

    @Qualifier("comparacaoTaskExecutor")
    private final Executor comparacaoTaskExecutor;

    public ComparacaoResponseDTO comparar(List<String> codigos) {
        List<String> tickers = normalizarCodigos(codigos);
        validarQuantidade(tickers);

        List<CompletableFuture<AcaoListagemResponseDTO>> futures = tickers.stream()
                .map(ticker -> CompletableFuture.supplyAsync(
                        () -> montarItemComparacao(ticker), comparacaoTaskExecutor))
                .toList();

        List<AcaoListagemResponseDTO> ativos = futures.stream()
                .map(this::joinOuRelancar)
                .toList();

        return ComparacaoResponseDTO.builder()
                .ativos(ativos)
                .build();
    }

    private List<String> normalizarCodigos(List<String> codigos) {
        return codigos.stream()
                .map(codigo -> codigo.toUpperCase().trim())
                .distinct()
                .toList();
    }

    private void validarQuantidade(List<String> tickers) {
        if (tickers.size() < MINIMO_ATIVOS) {
            throw new BusinessException("Informe pelo menos " + MINIMO_ATIVOS + " tickers para comparar");
        }
        if (tickers.size() > MAXIMO_ATIVOS) {
            throw new BusinessException("É possível comparar no máximo " + MAXIMO_ATIVOS + " ativos por vez");
        }
    }

    private AcaoListagemResponseDTO montarItemComparacao(String ticker) {
        Acao acao = acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Ativo não cadastrado ou inativo: " + ticker));

        AcaoListagemResponseDTO.AcaoListagemResponseDTOBuilder builder = AcaoListagemResponseDTO.builder()
                .id(acao.getId())
                .codigo(acao.getCodigo())
                .nome(acao.getNome())
                .tipo(acao.getTipo())
                .setor(acao.getSetor());

        try {
            CotacaoResponseDTO cotacao = cotacaoService.obterCotacao(acao.getCodigo());
            builder.preco(cotacao.getPreco())
                    .variacaoPercentual(cotacao.getVariacaoPercentual())
                    .dividendYield(cotacao.getDividendYield())
                    .precoValorPatrimonial(cotacao.getPrecoValorPatrimonial())
                    .volume(cotacao.getVolume())
                    .cotacaoDisponivel(true);
        } catch (ResourceNotFoundException e) {
            builder.cotacaoDisponivel(false);
        }

        return builder.build();
    }

    private AcaoListagemResponseDTO joinOuRelancar(CompletableFuture<AcaoListagemResponseDTO> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof ResourceNotFoundException resourceNotFoundException) {
                throw resourceNotFoundException;
            }
            throw e;
        }
    }
}
