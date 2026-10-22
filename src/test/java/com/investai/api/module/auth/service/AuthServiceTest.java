package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.infra.exception.MuitasTentativasException;
import com.investai.api.module.auth.dto.*;
import com.investai.api.module.auth.entity.RefreshToken;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.auth.service.LimiteTentativasService.Operacao;
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

    @Mock
    private LimiteTentativasService limiteTentativasService;

    private static final String IP = "200.10.20.30";

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

        LoginResponseDTO response = authService.login(dto, IP);

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
        verify(limiteTentativasService).verificar(LimiteTentativasService.Operacao.LOGIN, "lucas@email.com", IP);
        verify(limiteTentativasService).limpar(LimiteTentativasService.Operacao.LOGIN, "lucas@email.com");
        verify(limiteTentativasService, never()).registrarTentativa(any(), any(), any());
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

    @Test
    void deveRealizarLogout() {

        LogoutRequestDTO dto = LogoutRequestDTO.builder()
                .refreshToken("refresh-token")
                .build();

        authService.logout(dto);

        verify(refreshTokenService)
                .revogar("refresh-token");
    }

    @Test
    void devePropagarExcecaoAoRealizarLogout() {

        LogoutRequestDTO dto = LogoutRequestDTO.builder()
                .refreshToken("refresh-token")
                .build();

        doThrow(new BusinessException("Erro ao revogar token"))
                .when(refreshTokenService)
                .revogar("refresh-token");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.logout(dto)
        );

        assertEquals(
                "Erro ao revogar token",
                exception.getMessage()
        );

        verify(refreshTokenService)
                .revogar("refresh-token");
    }

    @Test
    void naoDeveRenovarSessaoDeContaDesativada() {
        RefreshRequestDTO dto = RefreshRequestDTO.builder().refreshToken("refresh-de-desativado").build();
        Usuario desativado = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .ativo(false)
                .role(Role.USUARIO)
                .build();
        RefreshToken tokenAtual = RefreshToken.builder()
                .token("refresh-de-desativado")
                .usuario(desativado)
                .revogado(false)
                .expiraEm(LocalDateTime.now().plusDays(7))
                .build();
        when(refreshTokenService.buscarValido("refresh-de-desativado")).thenReturn(tokenAtual);

        BusinessException erro = assertThrows(BusinessException.class, () -> authService.refresh(dto));

        assertEquals("Sua conta está desativada. Fale com o suporte da plataforma.", erro.getMessage());
        verify(jwtUtil, never()).gerarToken(any());
        verify(refreshTokenService, never()).criar(any());
    }

    private Usuario usuarioAtivo() {
        return Usuario.builder().id(UUID.randomUUID()).nome("Lucas").email("lucas@email.com")
                .senha("senha-criptografada").ativo(true).role(Role.USUARIO).build();
    }

    @Test
    void loginComSenhaErradaDeveContarATentativa() {
        LoginRequestDTO login = LoginRequestDTO.builder().email("Lucas@Email.com").senha("errada").build();
        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", usuario.getSenha())).thenReturn(false);

        BusinessException erro = assertThrows(BusinessException.class, () -> authService.login(login, IP));

        assertEquals("Credenciais inválidas", erro.getMessage());
        verify(limiteTentativasService).registrarTentativa(Operacao.LOGIN, "lucas@email.com", IP);
        verify(limiteTentativasService, never()).limpar(any(), any());
        verifyNoInteractions(jwtUtil, refreshTokenService);
    }

    @Test
    void loginComEmailInexistenteDeveContarIgualEDarAMesmaMensagem() {
        LoginRequestDTO login = LoginRequestDTO.builder().email("naoexiste@email.com").senha("123456").build();
        when(usuarioRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        BusinessException erro = assertThrows(BusinessException.class, () -> authService.login(login, IP));

        assertEquals("Credenciais inválidas", erro.getMessage());
        verify(limiteTentativasService).registrarTentativa(Operacao.LOGIN, "naoexiste@email.com", IP);
    }

    @Test
    void loginDeContaDesativadaDeveContarATentativa() {
        LoginRequestDTO login = LoginRequestDTO.builder().email("lucas@email.com").senha("123456").build();
        Usuario usuario = usuarioAtivo();
        usuario.setAtivo(false);
        when(usuarioRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", usuario.getSenha())).thenReturn(true);

        BusinessException erro = assertThrows(BusinessException.class, () -> authService.login(login, IP));

        assertEquals("Credenciais inválidas", erro.getMessage());
        verify(limiteTentativasService).registrarTentativa(Operacao.LOGIN, "lucas@email.com", IP);
    }

    @Test
    void loginBloqueadoNaoDeveNemConferirASenha() {
        LoginRequestDTO login = LoginRequestDTO.builder().email("lucas@email.com").senha("123456").build();
        doThrow(new MuitasTentativasException(900)).when(limiteTentativasService).verificar(Operacao.LOGIN, "lucas@email.com", IP);

        assertThrows(MuitasTentativasException.class, () -> authService.login(login, IP));

        verifyNoInteractions(usuarioRepository, passwordEncoder, jwtUtil, refreshTokenService);
        verify(limiteTentativasService, never()).registrarTentativa(any(), any(), any());
    }
}
