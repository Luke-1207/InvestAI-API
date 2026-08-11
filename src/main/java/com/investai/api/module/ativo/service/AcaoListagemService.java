package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.AcaoListagemFiltroDTO;
import com.investai.api.module.ativo.dto.AcaoListagemResponseDTO;
import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.repository.AcaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcaoListagemService {

    private final AcaoRepository acaoRepository;
    private final CotacaoService cotacaoService;

    public Page<AcaoListagemResponseDTO> listar(AcaoListagemFiltroDTO filtro) {
        List<Acao> candidatos = buscarCandidatos(filtro);

        List<AcaoListagemResponseDTO> combinados = candidatos.stream()
                .map(this::combinarComCotacao)
                .filter(dto -> passaNosFiltrosDeMercado(dto, filtro))
                .sorted(comparador(filtro))
                .toList();

        return paginar(combinados, filtro);
    }

    private List<Acao> buscarCandidatos(AcaoListagemFiltroDTO filtro) {
        boolean temTipo = filtro.getTipo() != null && !filtro.getTipo().isEmpty();
        boolean temSetor = filtro.getSetor() != null && !filtro.getSetor().isBlank();

        if (temTipo && temSetor) {
            return acaoRepository.findByAtivoTrueAndTipoInAndSetorIgnoreCase(filtro.getTipo(), filtro.getSetor());
        }
        if (temTipo) {
            return acaoRepository.findByAtivoTrueAndTipoIn(filtro.getTipo());
        }
        if (temSetor) {
            return acaoRepository.findByAtivoTrueAndSetorIgnoreCase(filtro.getSetor());
        }
        return acaoRepository.findByAtivoTrue();
    }

    private AcaoListagemResponseDTO combinarComCotacao(Acao acao) {
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

    private boolean passaNosFiltrosDeMercado(AcaoListagemResponseDTO dto, AcaoListagemFiltroDTO filtro) {
        if (filtro.getDyMinimo() != null) {
            if (dto.getDividendYield() == null || dto.getDividendYield().compareTo(filtro.getDyMinimo()) < 0) {
                return false;
            }
        }
        if (filtro.getPrecoMaximo() != null) {
            if (dto.getPreco() == null || dto.getPreco().compareTo(filtro.getPrecoMaximo()) > 0) {
                return false;
            }
        }
        return true;
    }

    private Comparator<? super AcaoListagemResponseDTO> comparador(AcaoListagemFiltroDTO filtro) {
        Comparator<AcaoListagemResponseDTO> comparator = switch (filtro.getOrdenarPor()) {
            case DY -> Comparator.comparing(
                    AcaoListagemResponseDTO::getDividendYield, Comparator.nullsLast(Comparator.naturalOrder()));
            case PRECO -> Comparator.comparing(
                    AcaoListagemResponseDTO::getPreco, Comparator.nullsLast(Comparator.naturalOrder()));
            case VARIACAO_DIA -> Comparator.comparing(
                    AcaoListagemResponseDTO::getVariacaoPercentual, Comparator.nullsLast(Comparator.naturalOrder()));
            case NOME -> Comparator.comparing(
                    AcaoListagemResponseDTO::getNome, String.CASE_INSENSITIVE_ORDER);
        };

        return filtro.getOrdem() == com.investai.api.module.ativo.dto.OrdemDTO.DESC
                ? comparator.reversed()
                : comparator;
    }

    private Page<AcaoListagemResponseDTO> paginar(List<AcaoListagemResponseDTO> lista, AcaoListagemFiltroDTO filtro) {
        int pagina = Math.max(filtro.getPagina(), 1) - 1;
        int tamanho = Math.max(filtro.getTamanho(), 1);

        PageRequest pageRequest = PageRequest.of(pagina, tamanho);
        int inicio = (int) pageRequest.getOffset();

        if (inicio >= lista.size()) {
            return new PageImpl<>(Collections.emptyList(), pageRequest, lista.size());
        }

        int fim = Math.min(inicio + tamanho, lista.size());
        return new PageImpl<>(lista.subList(inicio, fim), pageRequest, lista.size());
    }
}
