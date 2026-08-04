package com.investai.api.module.auth.controller;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.dto.*;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.auth.service.UsuarioService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
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

    @Test
    @DisplayName("GET /usuarios - deve retornar página de usuários para gestor")
    void listarUsuarios_deveRetornarPaginaComSucesso() throws Exception {
        UsuarioDetalheResponseDTO dto = criarDetalheResponseMock();
        Page<UsuarioDetalheResponseDTO> page = new PageImpl<>(List.of(dto));

        when(usuarioService.listarUsuarios(any(UsuarioFiltroDTO.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].nome").value(dto.getNome()));
    }

    @Test
    @DisplayName("GET /usuarios/{id} - deve retornar usuário por ID para gestor")
    void buscarUsuarioPorId_deveRetornarUsuarioComSucesso() throws Exception {
        UsuarioDetalheResponseDTO dto = criarDetalheResponseMock();
        UUID id = UUID.randomUUID();

        when(usuarioService.buscarUsuarioPorId(id)).thenReturn(dto);

        mockMvc.perform(get("/v1/usuarios/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(dto.getNome()))
                .andExpect(jsonPath("$.email").value(dto.getEmail()));
    }

    @Test
    @DisplayName("GET /usuarios/{id} - deve retornar 404 quando não encontrado")
    void buscarUsuarioPorId_deveRetornar404QuandoNaoEncontrado() throws Exception {
        UUID id = UUID.randomUUID();

        when(usuarioService.buscarUsuarioPorId(id))
                .thenThrow(new ResourceNotFoundException("Usuário não encontrado"));

        mockMvc.perform(get("/v1/usuarios/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /usuarios/{id}/role - deve alterar role com sucesso")
    void alterarRole_deveAlterarComSucesso() throws Exception {
        UsuarioDetalheResponseDTO dto = criarDetalheResponseMock();
        UUID id = UUID.randomUUID();

        when(usuarioService.alterarRole(eq(id), eq(Role.GESTOR))).thenReturn(dto);

        mockMvc.perform(patch("/v1/usuarios/{id}/role", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                { "role": "GESTOR" }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(dto.getNome()));
    }

    @Test
    @DisplayName("PATCH /usuarios/{id}/role - deve retornar 422 ao alterar a própria role")
    void alterarRole_deveRetornar422AoAlterarPropria() throws Exception {
        UUID id = UUID.randomUUID();

        when(usuarioService.alterarRole(eq(id), any()))
                .thenThrow(new BusinessException("Você não pode alterar a própria role"));

        mockMvc.perform(patch("/v1/usuarios/{id}/role", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                { "role": "USUARIO" }
            """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /usuarios/{id}/role - deve retornar 400 quando role não informada")
    void alterarRole_deveRetornar400QuandoRoleNula() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/v1/usuarios/{id}/role", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                { "role": null }
            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /usuarios/{id}/status - deve desativar usuário com sucesso")
    void alterarStatus_deveDesativarComSucesso() throws Exception {
        UsuarioDetalheResponseDTO dto = criarDetalheResponseMock();
        UUID id = UUID.randomUUID();

        when(usuarioService.alterarStatus(eq(id), eq(false))).thenReturn(dto);

        mockMvc.perform(patch("/v1/usuarios/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                { "ativo": false }
            """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /usuarios/{id}/status - deve retornar 422 ao desativar a própria conta")
    void alterarStatus_deveRetornar422AoDesativarPropria() throws Exception {
        UUID id = UUID.randomUUID();

        when(usuarioService.alterarStatus(eq(id), anyBoolean()))
                .thenThrow(new BusinessException("Você não pode alterar o status da própria conta"));

        mockMvc.perform(patch("/v1/usuarios/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                { "ativo": false }
            """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /usuarios/{id}/status - deve retornar 400 quando status não informado")
    void alterarStatus_deveRetornar400QuandoStatusNulo() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/v1/usuarios/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                { "ativo": null }
            """))
                .andExpect(status().isBadRequest());
    }

    private UsuarioDetalheResponseDTO criarDetalheResponseMock() {
        return UsuarioDetalheResponseDTO.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Silva")
                .email("lucas@email.com")
                .role(Role.USUARIO)
                .ativo(true)
                .perfilPreenchido(false)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }
}