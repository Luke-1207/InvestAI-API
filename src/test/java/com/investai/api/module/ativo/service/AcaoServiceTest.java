package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.ConflictException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.AcaoResponseDTO;
import com.investai.api.module.ativo.dto.AtualizarAcaoRequestDTO;
import com.investai.api.module.ativo.dto.CadastroAcaoRequestDTO;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcaoServiceTest {

    @Mock
    private AcaoRepository acaoRepository;

    @InjectMocks
    private AcaoService acaoService;

    @Test
    @DisplayName("cadastrar - deve criar ativo com sucesso e salvar código em maiúsculo")
    void cadastrar_deveCriarComSucessoESalvarCodigoEmMaiusculo() {
        CadastroAcaoRequestDTO dto = new CadastroAcaoRequestDTO();
        dto.setCodigo("taee3");
        dto.setNome("Taesa - Transmissão de Energia");
        dto.setTipo(TipoAtivo.ACAO);
        dto.setSetor("Energia Elétrica");

        when(acaoRepository.existsByCodigo("TAEE3")).thenReturn(false);
        when(acaoRepository.save(any(Acao.class))).thenAnswer(inv -> inv.getArgument(0));

        AcaoResponseDTO response = acaoService.cadastrar(dto);

        assertThat(response.getCodigo()).isEqualTo("TAEE3");
        assertThat(response.getNome()).isEqualTo("Taesa - Transmissão de Energia");
        assertThat(response.getTipo()).isEqualTo(TipoAtivo.ACAO);
        assertThat(response.isAtivo()).isTrue();

        ArgumentCaptor<Acao> captor = ArgumentCaptor.forClass(Acao.class);
        verify(acaoRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("TAEE3");
    }

    @Test
    @DisplayName("cadastrar - deve lançar exceção quando código já cadastrado")
    void cadastrar_deveLancarExcecaoQuandoCodigoJaCadastrado() {
        CadastroAcaoRequestDTO dto = new CadastroAcaoRequestDTO();
        dto.setCodigo("PETR4");
        dto.setNome("Petrobras");
        dto.setTipo(TipoAtivo.ACAO);

        when(acaoRepository.existsByCodigo("PETR4")).thenReturn(true);

        assertThatThrownBy(() -> acaoService.cadastrar(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um ativo cadastrado com esse código");

        verify(acaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar - deve atualizar ativo com sucesso")
    void atualizar_deveAtualizarComSucesso() {
        UUID id = UUID.randomUUID();
        Acao acao = criarAcaoMock(id);

        AtualizarAcaoRequestDTO dto = new AtualizarAcaoRequestDTO();
        dto.setNome("Taesa S.A.");
        dto.setTipo(TipoAtivo.ACAO);
        dto.setSetor("Energia");
        dto.setAtivo(true);

        when(acaoRepository.findById(id)).thenReturn(Optional.of(acao));
        when(acaoRepository.save(acao)).thenReturn(acao);

        AcaoResponseDTO response = acaoService.atualizar(id, dto);

        assertThat(response.getNome()).isEqualTo("Taesa S.A.");
        assertThat(response.getSetor()).isEqualTo("Energia");
        verify(acaoRepository).save(acao);
    }

    @Test
    @DisplayName("atualizar - deve lançar exceção quando ativo não encontrado")
    void atualizar_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        AtualizarAcaoRequestDTO dto = new AtualizarAcaoRequestDTO();
        dto.setNome("Qualquer");
        dto.setTipo(TipoAtivo.ACAO);
        dto.setAtivo(true);

        when(acaoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acaoService.atualizar(id, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ativo não encontrado");

        verify(acaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar - deve permitir reativar ativo desativado")
    void atualizar_devePermitirReativarAtivoDesativado() {
        UUID id = UUID.randomUUID();
        Acao acao = criarAcaoMock(id);
        acao.setAtivo(false);

        AtualizarAcaoRequestDTO dto = new AtualizarAcaoRequestDTO();
        dto.setNome(acao.getNome());
        dto.setTipo(acao.getTipo());
        dto.setSetor(acao.getSetor());
        dto.setAtivo(true);

        when(acaoRepository.findById(id)).thenReturn(Optional.of(acao));
        when(acaoRepository.save(acao)).thenReturn(acao);

        AcaoResponseDTO response = acaoService.atualizar(id, dto);

        assertThat(response.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("desativar - deve marcar ativo como inativo (soft delete)")
    void desativar_deveMarcarComoInativo() {
        UUID id = UUID.randomUUID();
        Acao acao = criarAcaoMock(id);

        when(acaoRepository.findById(id)).thenReturn(Optional.of(acao));
        when(acaoRepository.save(acao)).thenReturn(acao);

        acaoService.desativar(id);

        assertThat(acao.isAtivo()).isFalse();
        verify(acaoRepository).save(acao);
    }

    @Test
    @DisplayName("desativar - deve lançar exceção quando ativo não encontrado")
    void desativar_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();

        when(acaoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acaoService.desativar(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ativo não encontrado");

        verify(acaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("buscarPorId - deve retornar ativo quando encontrado")
    void buscarPorId_deveRetornarQuandoEncontrado() {
        UUID id = UUID.randomUUID();
        Acao acao = criarAcaoMock(id);

        when(acaoRepository.findById(id)).thenReturn(Optional.of(acao));

        AcaoResponseDTO response = acaoService.buscarPorId(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getCodigo()).isEqualTo(acao.getCodigo());
    }

    @Test
    @DisplayName("buscarPorId - deve lançar exceção quando não encontrado")
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();

        when(acaoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acaoService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ativo não encontrado");
    }

    private Acao criarAcaoMock(UUID id) {
        return Acao.builder()
                .id(id)
                .codigo("TAEE3")
                .nome("Taesa - Transmissão de Energia")
                .tipo(TipoAtivo.ACAO)
                .setor("Energia Elétrica")
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }
}