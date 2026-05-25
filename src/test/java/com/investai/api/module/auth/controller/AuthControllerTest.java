package com.investai.api.module.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.dto.CadastroRequestDTO;
import com.investai.api.module.auth.dto.CadastroResponseDTO;
import com.investai.api.module.auth.dto.LoginRequestDTO;
import com.investai.api.module.auth.dto.LoginResponseDTO;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.service.AuthService;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.shared.security.JwtUtil;
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
import static org.mockito.Mockito.when;
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
}