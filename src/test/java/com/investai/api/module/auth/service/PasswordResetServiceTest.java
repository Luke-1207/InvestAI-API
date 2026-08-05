package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.mail.MailService;
import com.investai.api.module.auth.dto.EsqueciSenhaRequestDTO;
import com.investai.api.module.auth.dto.RedefinirSenhaRequestDTO;
import com.investai.api.module.auth.entity.PasswordResetToken;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.PasswordResetTokenRepository;
import com.investai.api.module.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "expirationMinutes", 30L);
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:4200");
    }

    @Test
    @DisplayName("solicitarRecuperacao - deve gerar token e enviar e-mail quando usuário existe e está ativo")
    void solicitarRecuperacao_deveGerarTokenEEnviarEmailQuandoUsuarioAtivo() {
        Usuario usuario = criarUsuarioMock();

        EsqueciSenhaRequestDTO dto = new EsqueciSenhaRequestDTO();
        dto.setEmail("Lucas@Email.com");

        when(usuarioRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacao(dto);

        verify(passwordResetTokenRepository).invalidarTokensAnteriores(usuario.getId());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken tokenSalvo = tokenCaptor.getValue();
        assertThat(tokenSalvo.getUsuario()).isEqualTo(usuario);
        assertThat(tokenSalvo.isUsado()).isFalse();
        assertThat(tokenSalvo.getToken()).isNotBlank();
        assertThat(tokenSalvo.getExpiraEm()).isAfter(LocalDateTime.now());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).enviarEmailRecuperacaoSenha(
                eq(usuario.getEmail()), eq(usuario.getNome()), linkCaptor.capture()
        );
        assertThat(linkCaptor.getValue())
                .startsWith("http://localhost:4200/redefinir-senha?token=")
                .contains(tokenSalvo.getToken());
    }

    @Test
    @DisplayName("solicitarRecuperacao - não deve fazer nada quando e-mail não existe")
    void solicitarRecuperacao_naoDeveFazerNadaQuandoEmailNaoExiste() {
        EsqueciSenhaRequestDTO dto = new EsqueciSenhaRequestDTO();
        dto.setEmail("naoexiste@email.com");

        when(usuarioRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        passwordResetService.solicitarRecuperacao(dto);

        verify(passwordResetTokenRepository, never()).invalidarTokensAnteriores(any());
        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).enviarEmailRecuperacaoSenha(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("solicitarRecuperacao - não deve enviar e-mail quando conta está desativada")
    void solicitarRecuperacao_naoDeveEnviarQuandoContaDesativada() {
        Usuario usuario = criarUsuarioMock();
        usuario.setAtivo(false);

        EsqueciSenhaRequestDTO dto = new EsqueciSenhaRequestDTO();
        dto.setEmail(usuario.getEmail());

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacao(dto);

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).enviarEmailRecuperacaoSenha(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("solicitarRecuperacao - não deve enviar e-mail quando conta foi excluída (soft delete)")
    void solicitarRecuperacao_naoDeveEnviarQuandoContaExcluida() {
        Usuario usuario = criarUsuarioMock();
        usuario.setDeletadoEm(LocalDateTime.now());

        EsqueciSenhaRequestDTO dto = new EsqueciSenhaRequestDTO();
        dto.setEmail(usuario.getEmail());

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacao(dto);

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).enviarEmailRecuperacaoSenha(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("redefinirSenha - deve redefinir senha com sucesso e revogar sessões")
    void redefinirSenha_deveRedefinirComSucesso() {
        Usuario usuario = criarUsuarioMock();
        PasswordResetToken token = criarTokenValidoMock(usuario);

        RedefinirSenhaRequestDTO dto = new RedefinirSenhaRequestDTO();
        dto.setToken(token.getToken());
        dto.setNovaSenha("novaSenha123");
        dto.setConfirmarNovaSenha("novaSenha123");

        when(passwordResetTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("senha-nova-encoded");

        passwordResetService.redefinirSenha(dto);

        assertThat(usuario.getSenha()).isEqualTo("senha-nova-encoded");
        assertThat(token.isUsado()).isTrue();

        verify(usuarioRepository).save(usuario);
        verify(passwordResetTokenRepository).save(token);
        verify(refreshTokenService).revogarTodos(usuario);
    }

    @Test
    @DisplayName("redefinirSenha - deve lançar exceção quando token não existe")
    void redefinirSenha_deveLancarExcecaoQuandoTokenNaoExiste() {
        RedefinirSenhaRequestDTO dto = new RedefinirSenhaRequestDTO();
        dto.setToken("token-inexistente");
        dto.setNovaSenha("novaSenha123");
        dto.setConfirmarNovaSenha("novaSenha123");

        when(passwordResetTokenRepository.findByToken("token-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Token inválido ou expirado");

        verify(usuarioRepository, never()).save(any());
        verify(refreshTokenService, never()).revogarTodos(any());
    }

    @Test
    @DisplayName("redefinirSenha - deve lançar exceção quando token já foi usado")
    void redefinirSenha_deveLancarExcecaoQuandoTokenJaUsado() {
        Usuario usuario = criarUsuarioMock();
        PasswordResetToken token = criarTokenValidoMock(usuario);
        token.setUsado(true);

        RedefinirSenhaRequestDTO dto = new RedefinirSenhaRequestDTO();
        dto.setToken(token.getToken());
        dto.setNovaSenha("novaSenha123");
        dto.setConfirmarNovaSenha("novaSenha123");

        when(passwordResetTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Token inválido ou expirado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("redefinirSenha - deve lançar exceção quando token está expirado")
    void redefinirSenha_deveLancarExcecaoQuandoTokenExpirado() {
        Usuario usuario = criarUsuarioMock();
        PasswordResetToken token = criarTokenValidoMock(usuario);
        token.setExpiraEm(LocalDateTime.now().minusMinutes(1));

        RedefinirSenhaRequestDTO dto = new RedefinirSenhaRequestDTO();
        dto.setToken(token.getToken());
        dto.setNovaSenha("novaSenha123");
        dto.setConfirmarNovaSenha("novaSenha123");

        when(passwordResetTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Token inválido ou expirado");
    }

    @Test
    @DisplayName("redefinirSenha - deve lançar exceção quando senhas não conferem")
    void redefinirSenha_deveLancarExcecaoQuandoSenhasNaoConferem() {
        Usuario usuario = criarUsuarioMock();
        PasswordResetToken token = criarTokenValidoMock(usuario);

        RedefinirSenhaRequestDTO dto = new RedefinirSenhaRequestDTO();
        dto.setToken(token.getToken());
        dto.setNovaSenha("novaSenha123");
        dto.setConfirmarNovaSenha("senhaDiferente");

        when(passwordResetTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("As senhas não conferem");

        verify(usuarioRepository, never()).save(any());
        verify(refreshTokenService, never()).revogarTodos(any());
    }

    private Usuario criarUsuarioMock() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Silva")
                .email("lucas@email.com")
                .senha("senha-antiga-encoded")
                .role(Role.USUARIO)
                .ativo(true)
                .build();
    }

    private PasswordResetToken criarTokenValidoMock(Usuario usuario) {
        return PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .usado(false)
                .expiraEm(LocalDateTime.now().plusMinutes(30))
                .build();
    }
}