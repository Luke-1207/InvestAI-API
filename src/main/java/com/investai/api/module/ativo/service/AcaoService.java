package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.ConflictException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.AcaoResponseDTO;
import com.investai.api.module.ativo.dto.AtualizarAcaoRequestDTO;
import com.investai.api.module.ativo.dto.CadastroAcaoRequestDTO;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.repository.AcaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcaoService {

    private final AcaoRepository acaoRepository;

    @Transactional
    public AcaoResponseDTO cadastrar(CadastroAcaoRequestDTO dto) {
        String codigo = dto.getCodigo().toUpperCase().trim();

        if (acaoRepository.existsByCodigo(codigo)) {
            throw new ConflictException("Já existe um ativo cadastrado com esse código");
        }

        Acao acao = Acao.builder()
                .codigo(codigo)
                .nome(dto.getNome())
                .tipo(dto.getTipo())
                .setor(dto.getSetor())
                .ativo(true)
                .build();

        acaoRepository.save(acao);

        return toResponseDTO(acao);
    }

    @Transactional
    public AcaoResponseDTO atualizar(UUID id, AtualizarAcaoRequestDTO dto) {
        Acao acao = buscarOuLancar(id);

        acao.setNome(dto.getNome());
        acao.setTipo(dto.getTipo());
        acao.setSetor(dto.getSetor());
        acao.setAtivo(dto.getAtivo());

        acaoRepository.save(acao);

        return toResponseDTO(acao);
    }

    @Transactional
    public void desativar(UUID id) {
        Acao acao = buscarOuLancar(id);
        acao.setAtivo(false);
        acaoRepository.save(acao);
    }

    @Transactional(readOnly = true)
    public AcaoResponseDTO buscarPorId(UUID id) {
        return toResponseDTO(buscarOuLancar(id));
    }

    private Acao buscarOuLancar(UUID id) {
        return acaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ativo não encontrado"));
    }

    private AcaoResponseDTO toResponseDTO(Acao acao) {
        return AcaoResponseDTO.builder()
                .id(acao.getId())
                .codigo(acao.getCodigo())
                .nome(acao.getNome())
                .tipo(acao.getTipo())
                .setor(acao.getSetor())
                .ativo(acao.isAtivo())
                .criadoEm(acao.getCriadoEm())
                .atualizadoEm(acao.getAtualizadoEm())
                .build();
    }
}