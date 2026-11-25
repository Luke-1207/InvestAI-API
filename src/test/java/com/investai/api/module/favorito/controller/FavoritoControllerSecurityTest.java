package com.investai.api.module.favorito.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.favorito.service.FavoritoService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FavoritoController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class FavoritoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoritoService favoritoService;

    @MockitoBean
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private static final String TOKEN_USUARIO = "token-usuario-mock";

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Usuário Comum")
                .email("usuario@email.com")
                .senha("senha-encoded")
                .role(Role.USUARIO)
                .ativo(true)
                .build();
        when(jwtUtil.tokenValido(TOKEN_USUARIO)).thenReturn(true);
        when(jwtUtil.extrairEmail(TOKEN_USUARIO)).thenReturn(usuario.getEmail());
        when(usuarioDetailsService.loadUserByUsername(usuario.getEmail())).thenReturn(usuario);
    }

    @Test
    @DisplayName("GET /favoritos - deve retornar 401 sem token")
    void listar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/favoritos")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /favoritos - deve retornar 401 sem token")
    void adicionar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/v1/favoritos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /favoritos/{id} - deve retornar 401 sem token")
    void remover_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(delete("/v1/favoritos/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /favoritos - deve permitir usuário comum autenticado")
    void listar_devePermitirUsuarioComum() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());
        when(favoritoService.listar(any())).thenReturn(List.of());

        mockMvc.perform(get("/v1/favoritos").header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isOk());
    }
}
