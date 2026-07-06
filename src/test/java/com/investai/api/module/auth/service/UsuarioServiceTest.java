package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.module.auth.dto.AlterarSenhaRequestDTO;
import com.investai.api.module.auth.dto.AtualizarUsuarioRequestDTO;
import com.investai.api.module.auth.dto.UsuarioResponseDTO;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setup() {

        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .senha("senha-criptografada")
                .role(Role.USUARIO)
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();

    }

    @Test
    void deveObterUsuarioLogado() {

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        UsuarioResponseDTO response = usuarioService.obter();

        assertNotNull(response);
        assertEquals(usuario.getId(), response.getId());
        assertEquals(usuario.getNome(), response.getNome());
        assertEquals(usuario.getEmail(), response.getEmail());
        assertEquals(usuario.getRole(), response.getRole());
        assertEquals(usuario.isAtivo(), response.isAtivo());

        verify(usuarioAutenticadoHelper).getUsuarioLogado();
    }

    @Test
    void deveAtualizarUsuarioComSucesso() {

        AtualizarUsuarioRequestDTO dto =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Lucas Silva")
                        .email("lucasnovo@email.com")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(usuarioRepository.existsByEmail(dto.getEmail().toLowerCase()))
                .thenReturn(false);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuarioService.atualizar(dto);

        assertEquals("Lucas Silva", response.getNome());
        assertEquals("lucasnovo@email.com", response.getEmail());

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveAtualizarUsuarioMantendoMesmoEmail() {

        AtualizarUsuarioRequestDTO dto =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Novo Nome")
                        .email(usuario.getEmail())
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(usuarioRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.atualizar(dto);

        verify(usuarioRepository, never())
                .existsByEmail(anyString());

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveSalvarEmailEmLowerCaseAoAtualizar() {

        AtualizarUsuarioRequestDTO dto =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Lucas")
                        .email("LUCASNOVO@EMAIL.COM")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(usuarioRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(usuarioRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.atualizar(dto);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        assertEquals(
                "lucasnovo@email.com",
                captor.getValue().getEmail()
        );
    }

    @Test
    void deveLancarConflictExceptionQuandoEmailJaExistir() {

        AtualizarUsuarioRequestDTO dto =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Lucas")
                        .email("novo@email.com")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(usuarioRepository.existsByEmail("novo@email.com"))
                .thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> usuarioService.atualizar(dto)
        );

        assertEquals(
                "E-mail já está em uso",
                exception.getMessage()
        );

        verify(usuarioRepository, never())
                .save(any());
    }

    @Test
    void deveAlterarSenhaComSucesso() {

        AlterarSenhaRequestDTO dto =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("123456")
                        .novaSenha("654321")
                        .confirmarNovaSenha("654321")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(passwordEncoder.matches(
                "123456",
                usuario.getSenha()))
                .thenReturn(true);

        when(passwordEncoder.matches(
                "654321",
                usuario.getSenha()))
                .thenReturn(false);

        when(passwordEncoder.encode("654321"))
                .thenReturn("nova-senha-criptografada");

        usuarioService.alterarSenha(dto);

        assertEquals(
                "nova-senha-criptografada",
                usuario.getSenha()
        );

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveLancarBusinessExceptionQuandoSenhaAtualForIncorreta() {

        AlterarSenhaRequestDTO dto =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("errada")
                        .novaSenha("654321")
                        .confirmarNovaSenha("654321")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(passwordEncoder.matches(
                dto.getSenhaAtual(),
                usuario.getSenha()))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> usuarioService.alterarSenha(dto)
        );

        assertEquals(
                "Senha atual incorreta",
                exception.getMessage()
        );

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLancarBusinessExceptionQuandoConfirmacaoForDiferente() {

        AlterarSenhaRequestDTO dto =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("123456")
                        .novaSenha("654321")
                        .confirmarNovaSenha("999999")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(passwordEncoder.matches(
                dto.getSenhaAtual(),
                usuario.getSenha()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> usuarioService.alterarSenha(dto)
        );

        assertEquals(
                "As senhas não conferem",
                exception.getMessage()
        );

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLancarBusinessExceptionQuandoNovaSenhaForIgualAtual() {

        AlterarSenhaRequestDTO dto =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("123456")
                        .novaSenha("123456")
                        .confirmarNovaSenha("123456")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(passwordEncoder.matches(
                dto.getSenhaAtual(),
                usuario.getSenha()))
                .thenReturn(true);

        when(passwordEncoder.matches(
                dto.getNovaSenha(),
                usuario.getSenha()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> usuarioService.alterarSenha(dto)
        );

        assertEquals(
                "A nova senha deve ser diferente da senha atual",
                exception.getMessage()
        );

        verify(usuarioRepository, never()).save(any());
    }

}