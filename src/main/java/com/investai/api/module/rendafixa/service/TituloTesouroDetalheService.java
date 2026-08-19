package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.rendafixa.dto.DescricoesTesouro;
import com.investai.api.module.rendafixa.dto.TituloTesouroDetalheResponseDTO;
import com.investai.api.module.rendafixa.dto.ValorDescritoDTO;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TituloTesouroDetalheService {

    private static final String LIQUIDEZ_DIARIA = "DIARIA";

    private final TituloTesouroRepository tituloTesouroRepository;

    public TituloTesouroDetalheResponseDTO obterDetalhe(String codigo) {
        TituloTesouro titulo = tituloTesouroRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Título do Tesouro não encontrado: " + codigo));

        return TituloTesouroDetalheResponseDTO.builder()
                .codigo(titulo.getCodigo())
                .nome(titulo.getNome())
                .tipo(ValorDescritoDTO.builder()
                        .valor(titulo.getTipo().name())
                        .descricao(DescricoesTesouro.TIPO.get(titulo.getTipo()))
                        .build())
                .taxaAnual(titulo.getTaxaAnual())
                .precoMinimo(titulo.getPrecoMinimo())
                .vencimento(titulo.getVencimento())
                .pagaJurosSemestrais(titulo.isPagaJurosSemestrais())
                .liquidez(LIQUIDEZ_DIARIA)
                .resumoIA(null)
                .build();
    }
}