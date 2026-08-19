package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.rendafixa.dto.TituloPrivadoDetalheResponseDTO;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TituloPrivadoDetalheService {

    private final TituloPrivadoRepository tituloPrivadoRepository;
    private final CalculadoraRentabilidadeService calculadoraRentabilidadeService;

    public TituloPrivadoDetalheResponseDTO obterDetalhe(UUID id) {
        TituloPrivado titulo = tituloPrivadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Título privado não encontrado"));

        return TituloPrivadoDetalheResponseDTO.builder()
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
                .rentabilidadeEstimada(calculadoraRentabilidadeService.calcular(titulo))
                .resumoIA(null)
                .build();
    }
}