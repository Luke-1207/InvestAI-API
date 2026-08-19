package com.investai.api.module.rendafixa.service;

import com.investai.api.module.rendafixa.dto.TituloPrivadoListagemFiltroDTO;
import com.investai.api.module.rendafixa.dto.TituloPrivadoListagemResponseDTO;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
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
public class TituloPrivadoListagemService {

    private final TituloPrivadoRepository tituloPrivadoRepository;

    public Page<TituloPrivadoListagemResponseDTO> listar(TituloPrivadoListagemFiltroDTO filtro) {
        List<TituloPrivadoListagemResponseDTO> filtrados = tituloPrivadoRepository.findByAtivoTrue().stream()
                .filter(t -> filtro.getTipo() == null || filtro.getTipo().isEmpty() || filtro.getTipo().contains(t.getTipo()))
                .filter(t -> filtro.getIndexador() == null || t.getIndexador() == filtro.getIndexador())
                .filter(t -> filtro.getLiquidez() == null || t.getLiquidez() == filtro.getLiquidez())
                .filter(t -> filtro.getIsentoIR() == null || t.isIsentoIr() == filtro.getIsentoIR())
                .filter(t -> filtro.getInvestimentoMaximo() == null || t.getInvestimentoMinimo().compareTo(filtro.getInvestimentoMaximo()) <= 0)
                .filter(t -> filtro.getVencimentoAte() == null || !t.getVencimento().isAfter(filtro.getVencimentoAte()))
                .map(this::toListagemDTO)
                .sorted(comparador(filtro.getOrdenarPor()))
                .toList();

        return paginar(filtrados, filtro);
    }

    private TituloPrivadoListagemResponseDTO toListagemDTO(TituloPrivado titulo) {
        return TituloPrivadoListagemResponseDTO.builder()
                .id(titulo.getId())
                .tipo(titulo.getTipo())
                .emissor(titulo.getEmissor())
                .indexador(titulo.getIndexador())
                .taxaPercentual(titulo.getTaxaPercentual())
                .vencimento(titulo.getVencimento())
                .investimentoMinimo(titulo.getInvestimentoMinimo())
                .liquidez(titulo.getLiquidez())
                .garantidoFgc(titulo.isGarantidoFgc())
                .isentoIr(titulo.isIsentoIr())
                .build();
    }

    private Comparator<TituloPrivadoListagemResponseDTO> comparador(com.investai.api.module.rendafixa.dto.OrdenarPorTituloPrivado ordenarPor) {
        return switch (ordenarPor) {
            case TAXA -> Comparator.comparing(TituloPrivadoListagemResponseDTO::getTaxaPercentual,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case VENCIMENTO -> Comparator.comparing(TituloPrivadoListagemResponseDTO::getVencimento,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case INVESTIMENTO_MINIMO -> Comparator.comparing(TituloPrivadoListagemResponseDTO::getInvestimentoMinimo,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }

    private Page<TituloPrivadoListagemResponseDTO> paginar(List<TituloPrivadoListagemResponseDTO> lista, TituloPrivadoListagemFiltroDTO filtro) {
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