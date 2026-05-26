package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.module.auth.dto.*;
import com.investai.api.module.auth.entity.RefreshToken;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    private CadastroRequestDTO dto;

    @BeforeEach
    void setup() {
        dto = CadastroRequestDTO.builder()
                .nome("Lucas Fabiano")
                .email("lucas@email.com")
                .senha("123456")
                .confirmarSenha("123456")
                .build();
    }

    @Test
    void deveCadastrarUsuarioComSucesso() {

        when(usuarioRepository.existsByEmail(dto.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode(dto.getSenha()))
                .thenReturn("senha-criptografada");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> {
                    Usuario usuario = invocation.getArgument(0);
                    usuario.setId(UUID.randomUUID());
                    return usuario;
                });

        CadastroResponseDTO response = authService.cadastrar(dto);

        assertNotNull(response);
        assertNotNull(response.getId());

        assertEquals(dto.getNome(), response.getNome());
        assertEquals(dto.getEmail().toLowerCase(), response.getEmail());
        assertEquals(Role.USUARIO, response.getRole());

        verify(usuarioRepository).save(any(Usuario.class));
        verify(perfilInvestidorRepository)
                .save(any(PerfilInvestidor.class));
    }

    @Test
    void deveCriptografarSenhaAntesDeSalvar() {

        when(usuarioRepository.existsByEmail(dto.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode(dto.getSenha()))
                .thenReturn("senha-criptografada");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.cadastrar(dto);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioSalvo = captor.getValue();

        assertEquals(
                "senha-criptografada",
                usuarioSalvo.getSenha()
        );
    }

    @Test
    void deveSalvarUsuarioComRoleUsuario() {

        when(usuarioRepository.existsByEmail(dto.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("senha");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.cadastrar(dto);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuario = captor.getValue();

        assertEquals(Role.USUARIO, usuario.getRole());
        assertTrue(usuario.isAtivo());
    }

    @Test
    void deveLancarBusinessExceptionQuandoSenhasForemDiferentes() {

        dto.setConfirmarSenha("senha-diferente");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.cadastrar(dto)
        );

        assertEquals(
                "As senhas não conferem",
                exception.getMessage()
        );

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLancarConflictExceptionQuandoEmailJaExistir() {

        when(usuarioRepository.existsByEmail(dto.getEmail()))
                .thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> authService.cadastrar(dto)
        );

        assertEquals(
                "E-mail já cadastrado",
                exception.getMessage()
        );

        verify(usuarioRepository, never()).save(any());
        verify(perfilInvestidorRepository, never()).save(any());
    }

    @Test
    void deveSalvarPerfilInvestidorAoCadastrarUsuario() {

        when(usuarioRepository.existsByEmail(dto.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("senha");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.cadastrar(dto);

        ArgumentCaptor<PerfilInvestidor> captor =
                ArgumentCaptor.forClass(PerfilInvestidor.class);

        verify(perfilInvestidorRepository)
                .save(captor.capture());

        PerfilInvestidor perfil = captor.getValue();

        assertFalse(perfil.isPerfilPreenchido());
        assertNotNull(perfil.getUsuario());
    }

    @Test
    void deveSalvarEmailEmLowerCase() {

        dto.setEmail("LUCAS@EMAIL.COM");

        when(usuarioRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("senha");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.cadastrar(dto);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuario = captor.getValue();

        assertEquals(
                "lucas@email.com",
                usuario.getEmail()
        );
    }

    @Test
    void deveRealizarLoginComSucesso() {

        LoginRequestDTO dto = LoginRequestDTO.builder()
                .email("lucas@email.com")
                .senha("123456")
                .build();

        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .senha("senha-criptografada")
                .ativo(true)
                .role(Role.USUARIO)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .build();

        when(usuarioRepository.findByEmail("lucas@email.com"))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(
                dto.getSenha(),
                usuario.getSenha()))
                .thenReturn(true);

        when(jwtUtil.gerarToken(usuario))
                .thenReturn("access-token");

        when(jwtUtil.getExpirationMs())
                .thenReturn(3600000L);

        when(refreshTokenService.criar(usuario))
                .thenReturn(refreshToken);

        LoginResponseDTO response = authService.login(dto);

        assertNotNull(response);

        assertEquals(
                "access-token",
                response.getAccessToken()
        );

        assertEquals(
                "refresh-token",
                response.getRefreshToken()
        );

        assertEquals(
                3600L,
                response.getExpiresIn()
        );

        verify(refreshTokenService).criar(usuario);
        verify(jwtUtil).gerarToken(usuario);
    }

    @Test
    void deveRealizarRefreshComSucesso() {

        RefreshRequestDTO dto = RefreshRequestDTO.builder()
                .refreshToken("refresh-token-antigo")
                .build();

        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .ativo(true)
                .role(Role.USUARIO)
                .build();

        RefreshToken tokenAtual = RefreshToken.builder()
                .token("refresh-token-antigo")
                .usuario(usuario)
                .revogado(false)
                .expiraEm(LocalDateTime.now().plusDays(7))
                .build();

        RefreshToken novoRefreshToken = RefreshToken.builder()
                .token("novo-refresh-token")
                .usuario(usuario)
                .revogado(false)
                .expiraEm(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenService.buscarValido("refresh-token-antigo"))
                .thenReturn(tokenAtual);

        when(jwtUtil.gerarToken(usuario))
                .thenReturn("novo-access-token");

        when(jwtUtil.getExpirationMs())
                .thenReturn(3600000L);

        when(refreshTokenService.criar(usuario))
                .thenReturn(novoRefreshToken);

        LoginResponseDTO response = authService.refresh(dto);

        assertNotNull(response);

        assertEquals(
                "novo-access-token",
                response.getAccessToken()
        );

        assertEquals(
                "novo-refresh-token",
                response.getRefreshToken()
        );

        assertEquals(
                3600L,
                response.getExpiresIn()
        );

        verify(refreshTokenService)
                .buscarValido("refresh-token-antigo");

        verify(refreshTokenService)
                .revogar("refresh-token-antigo");

        verify(refreshTokenService)
                .criar(usuario);

        verify(jwtUtil)
                .gerarToken(usuario);
    }

    @Test
    void deveRevogarTokenAntigoAoRealizarRefresh() {

        RefreshRequestDTO dto = RefreshRequestDTO.builder()
                .refreshToken("refresh-antigo")
                .build();

        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .ativo(true)
                .build();

        RefreshToken tokenAtual = RefreshToken.builder()
                .token("refresh-antigo")
                .usuario(usuario)
                .revogado(false)
                .expiraEm(LocalDateTime.now().plusDays(7))
                .build();

        RefreshToken novoRefresh = RefreshToken.builder()
                .token("refresh-novo")
                .build();

        when(refreshTokenService.buscarValido("refresh-antigo"))
                .thenReturn(tokenAtual);

        when(jwtUtil.gerarToken(usuario))
                .thenReturn("access");

        when(jwtUtil.getExpirationMs())
                .thenReturn(3600000L);

        when(refreshTokenService.criar(usuario))
                .thenReturn(novoRefresh);

        authService.refresh(dto);

        verify(refreshTokenService)
                .revogar("refresh-antigo");
    }
}