package com.investai.api.module.auth.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.entity.UsuarioFoto;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.auth.service.UsuarioFotoService;
import com.investai.api.module.auth.service.UsuarioService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@WebMvcTest(controllers = UsuarioController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class UsuarioControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioFotoService usuarioFotoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

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
    @DisplayName("GET /usuarios - deve retornar 401 sem token")
    void listarUsuarios_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /usuarios - deve retornar 403 para usuário comum")
    void listarUsuarios_deveRetornar403ParaUsuarioComum() throws Exception {
        mockMvc.perform(get("/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /usuarios - deve retornar 200 para gestor autenticado")
    void listarUsuarios_deveRetornar200ParaGestor() throws Exception {
        mockMvc.perform(get("/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenGestor))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /usuarios/me - deve retornar 401 sem token")
    void excluirConta_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(delete("/v1/usuarios/me")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content("""
                { "senha": "qualquercoisa" }
            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /usuarios/me/foto - deve permitir usuário comum (não cai na regra de gestor)")
    void obterFoto_devePermitirUsuarioComum() throws Exception {
        when(usuarioFotoService.obter()).thenReturn(UsuarioFoto.builder()
                .usuarioId(UUID.randomUUID())
                .conteudo(new byte[]{1, 2, 3})
                .contentType("image/png")
                .build());

        mockMvc.perform(get("/v1/usuarios/me/foto")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /usuarios/me/foto - deve retornar 401 sem token")
    void removerFoto_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(delete("/v1/usuarios/me/foto"))
                .andExpect(status().isUnauthorized());
    }
}