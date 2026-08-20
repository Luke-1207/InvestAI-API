package com.investai.api.module.rendafixa.service;

import com.investai.api.module.rendafixa.dto.TituloTesouroListagemFiltroDTO;
import com.investai.api.module.rendafixa.dto.TituloTesouroListagemResponseDTO;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
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
public class TituloTesouroListagemService {

    private final TituloTesouroRepository tituloTesouroRepository;

    public Page<TituloTesouroListagemResponseDTO> listar(TituloTesouroListagemFiltroDTO filtro) {
        List<TituloTesouroListagemResponseDTO> filtrados = tituloTesouroRepository.findByDisponivelTrue().stream()
                .filter(t -> filtro.getTipo() == null || t.getTipo() == filtro.getTipo())
                .filter(t -> filtro.getVencimentoAte() == null || !t.getVencimento().isAfter(filtro.getVencimentoAte()))
                .filter(t -> filtro.getTaxaMinima() == null || t.getTaxaAnual().compareTo(filtro.getTaxaMinima()) >= 0)
                .map(this::toListagemDTO)
                .sorted(comparador(filtro.getOrdenarPor()))
                .toList();

        return paginar(filtrados, filtro);
    }

    private TituloTesouroListagemResponseDTO toListagemDTO(TituloTesouro titulo) {
        return TituloTesouroListagemResponseDTO.builder()
                .id(titulo.getId())
                .codigo(titulo.getCodigo())
                .nome(titulo.getNome())
                .tipo(titulo.getTipo())
                .taxaAnual(titulo.getTaxaAnual())
                .precoMinimo(titulo.getPrecoMinimo())
                .vencimento(titulo.getVencimento())
                .pagaJurosSemestrais(titulo.isPagaJurosSemestrais())
                .build();
    }

    private Comparator<TituloTesouroListagemResponseDTO> comparador(com.investai.api.module.rendafixa.dto.OrdenarPorTesouro ordenarPor) {
        return switch (ordenarPor) {
            case TAXA -> Comparator.comparing(TituloTesouroListagemResponseDTO::getTaxaAnual,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case PRECO_MINIMO -> Comparator.comparing(TituloTesouroListagemResponseDTO::getPrecoMinimo,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case VENCIMENTO -> Comparator.comparing(TituloTesouroListagemResponseDTO::getVencimento,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }

    private Page<TituloTesouroListagemResponseDTO> paginar(List<TituloTesouroListagemResponseDTO> lista, TituloTesouroListagemFiltroDTO filtro) {
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