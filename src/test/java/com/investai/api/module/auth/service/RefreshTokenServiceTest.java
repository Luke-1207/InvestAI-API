package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.module.auth.entity.RefreshToken;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.RefreshTokenRepository;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .ativo(true)
                .build();
    }

    @Test
    void deveCriarRefreshTokenComSucesso() {

        when(jwtUtil.getRefreshExpirationDays())
                .thenReturn(7L);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken response = refreshTokenService.criar(usuario);

        assertNotNull(response);
        assertNotNull(response.getToken());

        assertEquals(usuario, response.getUsuario());

        assertFalse(response.isRevogado());

        assertNotNull(response.getExpiraEm());

        verify(refreshTokenRepository)
                .save(any(RefreshToken.class));
    }

    @Test
    void deveDefinirDataDeExpiracaoCorretamenteAoCriarToken() {

        when(jwtUtil.getRefreshExpirationDays())
                .thenReturn(10L);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antes = LocalDateTime.now();

        RefreshToken response = refreshTokenService.criar(usuario);

        LocalDateTime depois = LocalDateTime.now();

        LocalDateTime expiracaoMinima = antes.plusDays(10);
        LocalDateTime expiracaoMaxima = depois.plusDays(10);

        assertTrue(
                !response.getExpiraEm().isBefore(expiracaoMinima)
        );

        assertTrue(
                !response.getExpiraEm().isAfter(expiracaoMaxima)
        );
    }

    @Test
    void deveBuscarRefreshTokenValidoComSucesso() {

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .usuario(usuario)
                .revogado(false)
                .expiraEm(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        RefreshToken response =
                refreshTokenService.buscarValido("refresh-token");

        assertNotNull(response);

        assertEquals(
                "refresh-token",
                response.getToken()
        );
    }

    @Test
    void deveLancarBusinessExceptionQuandoRefreshTokenNaoExistir() {

        when(refreshTokenRepository.findByToken("token-invalido"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.buscarValido("token-invalido")
        );

        assertEquals(
                "Refresh token inválido",
                exception.getMessage()
        );
    }

    @Test
    void deveLancarBusinessExceptionQuandoRefreshTokenEstiverRevogado() {

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .usuario(usuario)
                .revogado(true)
                .expiraEm(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.buscarValido("refresh-token")
        );

        assertEquals(
                "Refresh token expirado ou revogado",
                exception.getMessage()
        );
    }

    @Test
    void deveLancarBusinessExceptionQuandoRefreshTokenEstiverExpirado() {

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .usuario(usuario)
                .revogado(false)
                .expiraEm(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.buscarValido("refresh-token")
        );

        assertEquals(
                "Refresh token expirado ou revogado",
                exception.getMessage()
        );
    }

    @Test
    void deveRevogarRefreshTokenComSucesso() {

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .usuario(usuario)
                .revogado(false)
                .expiraEm(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        refreshTokenService.revogar("refresh-token");

        assertTrue(refreshToken.isRevogado());

        verify(refreshTokenRepository)
                .save(refreshToken);
    }

    @Test
    void naoDeveSalvarNadaQuandoRefreshTokenNaoExistirAoRevogar() {

        when(refreshTokenRepository.findByToken("token"))
                .thenReturn(Optional.empty());

        refreshTokenService.revogar("token");

        verify(refreshTokenRepository, never())
                .save(any());
    }

    @Test
    void deveRevogarTodosOsTokensDoUsuario() {

        refreshTokenService.revogarTodos(usuario);

        verify(refreshTokenRepository)
                .revogarTodosPorUsuario(usuario.getId());
    }
}