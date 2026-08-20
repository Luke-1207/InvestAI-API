package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.rendafixa.dto.AtualizarTituloPrivadoRequestDTO;
import com.investai.api.module.rendafixa.dto.CadastroTituloPrivadoRequestDTO;
import com.investai.api.module.rendafixa.dto.TituloPrivadoResponseDTO;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TituloPrivadoService {

    private final TituloPrivadoRepository tituloPrivadoRepository;

    @Transactional
    public TituloPrivadoResponseDTO cadastrar(CadastroTituloPrivadoRequestDTO dto) {
        TituloPrivado titulo = TituloPrivado.builder()
                .tipo(dto.getTipo())
                .emissor(dto.getEmissor())
                .indexador(dto.getIndexador())
                .taxaPercentual(dto.getTaxaPercentual())
                .vencimento(dto.getVencimento())
                .investimentoMinimo(dto.getInvestimentoMinimo())
                .liquidez(dto.getLiquidez())
                .garantidoFgc(dto.getGarantidoFgc())
                .isentoIr(dto.getIsentoIr())
                .ativo(true)
                .build();

        tituloPrivadoRepository.save(titulo);
        return toResponseDTO(titulo);
    }

    @Transactional
    public TituloPrivadoResponseDTO atualizar(UUID id, AtualizarTituloPrivadoRequestDTO dto) {
        TituloPrivado titulo = buscarOuLancar(id);

        titulo.setTipo(dto.getTipo());
        titulo.setEmissor(dto.getEmissor());
        titulo.setIndexador(dto.getIndexador());
        titulo.setTaxaPercentual(dto.getTaxaPercentual());
        titulo.setVencimento(dto.getVencimento());
        titulo.setInvestimentoMinimo(dto.getInvestimentoMinimo());
        titulo.setLiquidez(dto.getLiquidez());
        titulo.setGarantidoFgc(dto.getGarantidoFgc());
        titulo.setIsentoIr(dto.getIsentoIr());

        tituloPrivadoRepository.save(titulo);
        return toResponseDTO(titulo);
    }

    @Transactional
    public TituloPrivadoResponseDTO alterarStatus(UUID id, Boolean ativo) {
        TituloPrivado titulo = buscarOuLancar(id);
        titulo.setAtivo(ativo);
        tituloPrivadoRepository.save(titulo);
        return toResponseDTO(titulo);
    }

    @Transactional
    public void desativar(UUID id) {
        TituloPrivado titulo = buscarOuLancar(id);
        titulo.setAtivo(false);
        tituloPrivadoRepository.save(titulo);
    }

    private TituloPrivado buscarOuLancar(UUID id) {
        return tituloPrivadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Título privado não encontrado"));
    }

    private TituloPrivadoResponseDTO toResponseDTO(TituloPrivado titulo) {
        return TituloPrivadoResponseDTO.builder()
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
                .ativo(titulo.isAtivo())
                .criadoEm(titulo.getCriadoEm())
                .atualizadoEm(titulo.getAtualizadoEm())
                .build();
    }
}