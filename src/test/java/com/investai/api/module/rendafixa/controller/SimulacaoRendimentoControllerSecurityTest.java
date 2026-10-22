package com.investai.api.module.rendafixa.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.rendafixa.dto.SimulacaoRendimentoResponseDTO;
import com.investai.api.module.rendafixa.service.SimuladorRendimentoService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimulacaoRendimentoController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SimulacaoRendimentoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimuladorRendimentoService simuladorRendimentoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("GET simulacao - deve retornar 401 sem token")
    void simular_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/renda-fixa/titulos/{id}/simulacao", UUID.randomUUID())
                        .param("valor", "1000").param("prazoMeses", "12"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET simulacao - qualquer usuário autenticado pode simular, não só o gestor")
    void simular_devePermitirUsuarioComum() throws Exception {
        when(jwtUtil.tokenValido("token-valido")).thenReturn(true);
        when(jwtUtil.extrairEmail("token-valido")).thenReturn("usuario@email.com");
        when(usuarioDetailsService.loadUserByUsername("usuario@email.com")).thenReturn(Usuario.builder()
                .id(UUID.randomUUID()).nome("Usuário").email("usuario@email.com").senha("x")
                .role(Role.USUARIO).ativo(true).build());
        when(simuladorRendimentoService.simularTituloPrivado(any(), any()))
                .thenReturn(SimulacaoRendimentoResponseDTO.builder().build());

        mockMvc.perform(get("/v1/renda-fixa/titulos/{id}/simulacao", UUID.randomUUID())
                        .header("Authorization", "Bearer token-valido")
                        .param("valor", "1000").param("prazoMeses", "12"))
                .andExpect(status().isOk());
    }
}
