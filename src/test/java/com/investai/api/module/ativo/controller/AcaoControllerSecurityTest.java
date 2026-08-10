package com.investai.api.module.ativo.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.ativo.dto.AcaoResponseDTO;
import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.service.AcaoListagemService;
import com.investai.api.module.ativo.service.AcaoService;
import com.investai.api.module.ativo.service.CotacaoService;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AcaoController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AcaoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcaoService acaoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @MockitoBean
    private CotacaoService cotacaoService;

    @MockitoBean
    private AcaoListagemService acaoListagemService;

    private String tokenUsuario;
    private String tokenGestor;

    @BeforeEach
    void setUp() {
        tokenUsuario = "token-usuario-mock";
        tokenGestor  = "token-gestor-mock";

        Usuario usuarioComum = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Silva")
                .email("lucas@email.com")
                .senha("senha-encoded")
                .role(Role.USUARIO)
                .ativo(true)
                .build();

        Usuario gestorMock = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Admin Gestor")
                .email("gestor@email.com")
                .senha("senha-encoded")
                .role(Role.GESTOR)
                .ativo(true)
                .build();

        when(jwtUtil.tokenValido(tokenUsuario)).thenReturn(true);
        when(jwtUtil.tokenValido(tokenGestor)).thenReturn(true);

        when(jwtUtil.extrairEmail(tokenUsuario)).thenReturn(usuarioComum.getEmail());
        when(jwtUtil.extrairEmail(tokenGestor)).thenReturn(gestorMock.getEmail());

        when(usuarioDetailsService.loadUserByUsername(usuarioComum.getEmail()))
                .thenReturn(usuarioComum);
        when(usuarioDetailsService.loadUserByUsername(gestorMock.getEmail()))
                .thenReturn(gestorMock);
    }

    @Test
    @DisplayName("POST /acoes - deve retornar 401 sem token")
    void cadastrar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/v1/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /acoes - deve retornar 403 para usuário comum")
    void cadastrar_deveRetornar403ParaUsuarioComum() throws Exception {
        mockMvc.perform(post("/v1/acoes")
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /acoes - deve permitir gestor autenticado (não barrado por segurança)")
    void cadastrar_devePermitirGestor() throws Exception {
        when(acaoService.cadastrar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(criarResponseMock());

        String corpoValido = """
            { "codigo": "TAEE3", "nome": "Taesa", "tipo": "ACAO", "setor": "Energia" }
        """;

        mockMvc.perform(post("/v1/acoes")
                        .header("Authorization", "Bearer " + tokenGestor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /acoes/{id} - deve retornar 401 sem token")
    void atualizar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(put("/v1/acoes/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /acoes/{id} - deve retornar 403 para usuário comum")
    void atualizar_deveRetornar403ParaUsuarioComum() throws Exception {
        mockMvc.perform(put("/v1/acoes/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /acoes/{id} - deve retornar 401 sem token")
    void desativar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(delete("/v1/acoes/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /acoes/{id} - deve retornar 403 para usuário comum")
    void desativar_deveRetornar403ParaUsuarioComum() throws Exception {
        mockMvc.perform(delete("/v1/acoes/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /acoes/{id} - deve retornar 401 sem token")
    void buscarPorId_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/acoes/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /acoes/{id} - deve permitir usuário comum autenticado")
    void buscarPorId_devePermitirUsuarioComum() throws Exception {
        when(acaoService.buscarPorId(org.mockito.ArgumentMatchers.any()))
                .thenReturn(criarResponseMock());

        mockMvc.perform(get("/v1/acoes/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/cotacao - deve retornar 401 sem token")
    void obterCotacao_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/acoes/{codigo}/cotacao", "TAEE3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/cotacao - deve permitir usuário comum autenticado")
    void obterCotacao_devePermitirUsuarioComum() throws Exception {
        when(cotacaoService.obterCotacao(org.mockito.ArgumentMatchers.any()))
                .thenReturn(CotacaoResponseDTO.builder().codigo("TAEE3").build());

        mockMvc.perform(get("/v1/acoes/{codigo}/cotacao", "TAEE3")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /acoes - deve retornar 401 sem token")
    void listar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/acoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /acoes - deve permitir usuário comum autenticado")
    void listar_devePermitirUsuarioComum() throws Exception {
        when(acaoListagemService.listar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));

        mockMvc.perform(get("/v1/acoes")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    private AcaoResponseDTO criarResponseMock() {
        return AcaoResponseDTO.builder()
                .id(UUID.randomUUID())
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