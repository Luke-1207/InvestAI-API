package com.investai.api.module.rendafixa.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.rendafixa.service.RendaFixaUnificadaService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RendaFixaUnificadaController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class RendaFixaUnificadaControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RendaFixaUnificadaService rendaFixaUnificadaService;

    @MockitoBean
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("GET /renda-fixa - deve retornar 401 sem token")
    void listar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/renda-fixa"))
                .andExpect(status().isUnauthorized());
    }
}