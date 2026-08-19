package com.investai.api.module.rendafixa.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.rendafixa.service.TituloTesouroDetalheService;
import com.investai.api.module.rendafixa.service.TituloTesouroListagemService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TesouroDiretoController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class TesouroDiretoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TituloTesouroListagemService tituloTesouroListagemService;

    @MockitoBean
    private TituloTesouroDetalheService tituloTesouroDetalheService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("GET /renda-fixa/tesouro - deve retornar 401 sem token")
    void listar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/renda-fixa/tesouro"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /renda-fixa/tesouro - deve permitir usuário autenticado (sem restrição de role)")
    void listar_devePermitirUsuarioAutenticado() throws Exception {
        when(jwtUtil.tokenValido("token-valido")).thenReturn(true);
        when(jwtUtil.extrairEmail("token-valido")).thenReturn("usuario@email.com");

        com.investai.api.module.auth.entity.Usuario usuario = com.investai.api.module.auth.entity.Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Usuário Teste")
                .email("usuario@email.com")
                .senha("senha-encoded")
                .role(com.investai.api.module.auth.entity.Role.USUARIO)
                .ativo(true)
                .build();

        when(usuarioDetailsService.loadUserByUsername("usuario@email.com")).thenReturn(usuario);
        when(tituloTesouroListagemService.listar(any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/v1/renda-fixa/tesouro")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk());
    }
}