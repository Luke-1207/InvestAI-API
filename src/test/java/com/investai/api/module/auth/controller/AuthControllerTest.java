package com.investai.api.module.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.dto.*;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.service.AuthService;
import com.investai.api.module.auth.service.PasswordResetService;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    void deveCadastrarUsuarioComSucesso() throws Exception {

        CadastroRequestDTO request = CadastroRequestDTO.builder()
                .nome("Lucas")
                .email("lucas@email.com")
                .senha("12345678")
                .confirmarSenha("12345678")
                .build();

        CadastroResponseDTO response = CadastroResponseDTO.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .role(Role.USUARIO)
                .build();

        when(authService.cadastrar(any(CadastroRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Lucas"))
                .andExpect(jsonPath("$.email").value("lucas@email.com"))
                .andExpect(jsonPath("$.role").value("USUARIO"));
    }

    @Test
    void deveRetornar400QuandoRequestForInvalido() throws Exception {

        CadastroRequestDTO request = CadastroRequestDTO.builder()
                .nome("")
                .email("email-invalido")
                .senha("")
                .confirmarSenha("")
                .build();

        mockMvc.perform(post("/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("Erro de validação"));
    }

    @Test
    void deveRetornar422QuandoServiceLancarBusinessException() throws Exception {

        CadastroRequestDTO request = CadastroRequestDTO.builder()
                .nome("Lucas")
                .email("lucas@email.com")
                .senha("12345678")
                .confirmarSenha("87654321")
                .build();

        when(authService.cadastrar(any(CadastroRequestDTO.class)))
                .thenThrow(new BusinessException("As senhas não conferem"));

        mockMvc.perform(post("/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro")
                        .value("As senhas não conferem"));
    }

    @Test
    void deveRealizarLoginComSucesso() throws Exception {

        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("lucas@email.com")
                .senha("12345678")
                .build();

        LoginResponseDTO response = LoginResponseDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .build();

        when(authService.login(any(LoginRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("refresh-token"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(3600));
    }

    @Test
    void deveRetornar400QuandoLoginRequestForInvalido() throws Exception {

        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("email-invalido")
                .senha("")
                .build();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("Erro de validação"));
    }

    @Test
    void deveRetornar422QuandoCredenciaisForemInvalidas() throws Exception {

        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("lucas@email.com")
                .senha("senha-errada")
                .build();

        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new BusinessException("Credenciais inválidas"));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro")
                        .value("Credenciais inválidas"));
    }

    @Test
    void deveRetornar422QuandoUsuarioEstiverDesabilitado() throws Exception {

        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("lucas@email.com")
                .senha("12345678")
                .build();

        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new BusinessException("Credenciais inválidas"));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro")
                        .value("Credenciais inválidas"));
    }

    @Test
    void deveRealizarRefreshComSucesso() throws Exception {

        RefreshRequestDTO request = RefreshRequestDTO.builder()
                .refreshToken("refresh-token")
                .build();

        LoginResponseDTO response = LoginResponseDTO.builder()
                .accessToken("novo-access-token")
                .refreshToken("novo-refresh-token")
                .expiresIn(3600L)
                .build();

        when(authService.refresh(any(RefreshRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("novo-access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("novo-refresh-token"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(3600));
    }

    @Test
    void deveRetornar400QuandoRefreshRequestForInvalido() throws Exception {

        RefreshRequestDTO request = RefreshRequestDTO.builder()
                .refreshToken("")
                .build();

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("Erro de validação"));
    }

    @Test
    void deveRetornar422QuandoRefreshTokenForInvalido() throws Exception {

        RefreshRequestDTO request = RefreshRequestDTO.builder()
                .refreshToken("token-invalido")
                .build();

        when(authService.refresh(any(RefreshRequestDTO.class)))
                .thenThrow(new BusinessException("Refresh token inválido"));

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro")
                        .value("Refresh token inválido"));
    }

    @Test
    void deveRetornar422QuandoRefreshTokenEstiverExpiradoOuRevogado() throws Exception {

        RefreshRequestDTO request = RefreshRequestDTO.builder()
                .refreshToken("refresh-token")
                .build();

        when(authService.refresh(any(RefreshRequestDTO.class)))
                .thenThrow(
                        new BusinessException(
                                "Refresh token expirado ou revogado"
                        )
                );

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro")
                        .value("Refresh token expirado ou revogado"));
    }

    @Test
    void deveRealizarLogoutComSucesso() throws Exception {

        LogoutRequestDTO request = LogoutRequestDTO.builder()
                .refreshToken("refresh-token")
                .build();

        mockMvc.perform(post("/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar400QuandoLogoutRequestForInvalido() throws Exception {

        LogoutRequestDTO request = LogoutRequestDTO.builder()
                .refreshToken("")
                .build();

        mockMvc.perform(post("/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("Erro de validação"));
    }

    @Test
    void deveRetornar422QuandoLogoutFalhar() throws Exception {

        LogoutRequestDTO request = LogoutRequestDTO.builder()
                .refreshToken("refresh-token")
                .build();

        doThrow(new BusinessException("Refresh token inválido"))
                .when(authService)
                .logout(any(LogoutRequestDTO.class));

        mockMvc.perform(post("/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro")
                        .value("Refresh token inválido"));
    }

    @Test
    @DisplayName("POST /auth/esqueci-senha - deve retornar 200 com mensagem genérica")
    void esqueciSenha_deveRetornar200ComMensagemGenerica() throws Exception {

        EsqueciSenhaRequestDTO request = new EsqueciSenhaRequestDTO();
        request.setEmail("lucas@email.com");

        mockMvc.perform(post("/v1/auth/esqueci-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem")
                        .value("Se o e-mail estiver cadastrado, você receberá instruções em instantes."));

        verify(passwordResetService).solicitarRecuperacao(any(EsqueciSenhaRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/esqueci-senha - deve retornar 200 mesmo com e-mail inexistente")
    void esqueciSenha_deveRetornar200ComEmailInexistente() throws Exception {

        EsqueciSenhaRequestDTO request = new EsqueciSenhaRequestDTO();
        request.setEmail("naoexiste@email.com");

        mockMvc.perform(post("/v1/auth/esqueci-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/esqueci-senha - deve retornar 400 quando e-mail inválido")
    void esqueciSenha_deveRetornar400QuandoEmailInvalido() throws Exception {

        EsqueciSenhaRequestDTO request = new EsqueciSenhaRequestDTO();
        request.setEmail("nao-e-um-email");

        mockMvc.perform(post("/v1/auth/esqueci-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/redefinir-senha - deve retornar 204 quando redefinição é bem sucedida")
    void redefinirSenha_deveRetornar204ComSucesso() throws Exception {

        RedefinirSenhaRequestDTO request = new RedefinirSenhaRequestDTO();
        request.setToken("token-valido");
        request.setNovaSenha("novaSenha123");
        request.setConfirmarNovaSenha("novaSenha123");

        mockMvc.perform(post("/v1/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(passwordResetService).redefinirSenha(any(RedefinirSenhaRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/redefinir-senha - deve retornar 422 quando token inválido ou expirado")
    void redefinirSenha_deveRetornar422QuandoTokenInvalido() throws Exception {

        RedefinirSenhaRequestDTO request = new RedefinirSenhaRequestDTO();
        request.setToken("token-invalido");
        request.setNovaSenha("novaSenha123");
        request.setConfirmarNovaSenha("novaSenha123");

        doThrow(new BusinessException("Token inválido ou expirado"))
                .when(passwordResetService)
                .redefinirSenha(any(RedefinirSenhaRequestDTO.class));

        mockMvc.perform(post("/v1/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("Token inválido ou expirado"));
    }

    @Test
    @DisplayName("POST /auth/redefinir-senha - deve retornar 422 quando senhas não conferem")
    void redefinirSenha_deveRetornar422QuandoSenhasNaoConferem() throws Exception {

        RedefinirSenhaRequestDTO request = new RedefinirSenhaRequestDTO();
        request.setToken("token-valido");
        request.setNovaSenha("novaSenha123");
        request.setConfirmarNovaSenha("outraSenha456");

        doThrow(new BusinessException("As senhas não conferem"))
                .when(passwordResetService)
                .redefinirSenha(any(RedefinirSenhaRequestDTO.class));

        mockMvc.perform(post("/v1/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("As senhas não conferem"));
    }

    @Test
    @DisplayName("POST /auth/redefinir-senha - deve retornar 400 quando token não informado")
    void redefinirSenha_deveRetornar400QuandoTokenVazio() throws Exception {

        RedefinirSenhaRequestDTO request = new RedefinirSenhaRequestDTO();
        request.setToken("");
        request.setNovaSenha("novaSenha123");
        request.setConfirmarNovaSenha("novaSenha123");

        mockMvc.perform(post("/v1/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/redefinir-senha - deve retornar 400 quando nova senha for muito curta")
    void redefinirSenha_deveRetornar400QuandoSenhaCurta() throws Exception {

        RedefinirSenhaRequestDTO request = new RedefinirSenhaRequestDTO();
        request.setToken("token-valido");
        request.setNovaSenha("abc123");
        request.setConfirmarNovaSenha("abc123");

        mockMvc.perform(post("/v1/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}