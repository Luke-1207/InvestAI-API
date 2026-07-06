package com.investai.api.module.auth.controller;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.dto.AlterarSenhaRequestDTO;
import com.investai.api.module.auth.dto.AtualizarUsuarioRequestDTO;
import com.investai.api.module.auth.dto.UsuarioResponseDTO;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.auth.service.UsuarioService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void deveObterUsuarioLogadoComSucesso() throws Exception {

        UsuarioResponseDTO response = UsuarioResponseDTO.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .role(Role.USUARIO)
                .ativo(true)
                .build();

        when(usuarioService.obter())
                .thenReturn(response);

        mockMvc.perform(get("/v1/usuarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Lucas"))
                .andExpect(jsonPath("$.email").value("lucas@email.com"))
                .andExpect(jsonPath("$.role").value("USUARIO"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveAtualizarUsuarioComSucesso() throws Exception {

        AtualizarUsuarioRequestDTO request =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Lucas Silva")
                        .email("lucas@email.com")
                        .build();

        UsuarioResponseDTO response = UsuarioResponseDTO.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Silva")
                .email("lucas@email.com")
                .role(Role.USUARIO)
                .ativo(true)
                .build();

        when(usuarioService.atualizar(any()))
                .thenReturn(response);

        mockMvc.perform(put("/v1/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome")
                        .value("Lucas Silva"))
                .andExpect(jsonPath("$.email")
                        .value("lucas@email.com"));
    }

    @Test
    void deveRetornar400QuandoAtualizacaoForInvalida() throws Exception {

        AtualizarUsuarioRequestDTO request =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("")
                        .email("email-invalido")
                        .build();

        mockMvc.perform(put("/v1/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("Erro de validação"));
    }

    @Test
    void deveRetornar422QuandoAtualizacaoLancarBusinessException() throws Exception {

        AtualizarUsuarioRequestDTO request =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Lucas")
                        .email("lucas@email.com")
                        .build();

        when(usuarioService.atualizar(any()))
                .thenThrow(new BusinessException("Erro ao atualizar"));

        mockMvc.perform(put("/v1/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro")
                        .value("Erro ao atualizar"));
    }

    @Test
    void deveAlterarSenhaComSucesso() throws Exception {

        AlterarSenhaRequestDTO request =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("12345678")
                        .novaSenha("87654321")
                        .confirmarNovaSenha("87654321")
                        .build();

        mockMvc.perform(patch("/v1/usuarios/me/senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(usuarioService)
                .alterarSenha(any(AlterarSenhaRequestDTO.class));
    }

    @Test
    void deveRetornar400QuandoAlteracaoSenhaForInvalida() throws Exception {

        AlterarSenhaRequestDTO request =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("")
                        .novaSenha("")
                        .confirmarNovaSenha("")
                        .build();

        mockMvc.perform(patch("/v1/usuarios/me/senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("Erro de validação"));
    }

    @Test
    void deveRetornar422QuandoAlterarSenhaLancarBusinessException() throws Exception {

        AlterarSenhaRequestDTO request =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("12345678")
                        .novaSenha("87654321")
                        .confirmarNovaSenha("87654321")
                        .build();

        doThrow(new BusinessException("Senha atual incorreta"))
                .when(usuarioService)
                .alterarSenha(any(AlterarSenhaRequestDTO.class));

        mockMvc.perform(patch("/v1/usuarios/me/senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro")
                        .value("Senha atual incorreta"));
    }

}