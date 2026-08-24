package com.investai.api.module.dashboard.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.dashboard.dto.DashboardAdminResponseDTO;
import com.investai.api.module.dashboard.service.DashboardAdminService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@WebMvcTest(controllers = DashboardAdminController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class DashboardAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardAdminService dashboardAdminService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private String tokenUsuario;
    private String tokenGestor;

    @BeforeEach
    void setUp() {
        tokenUsuario = "token-usuario-mock";
        tokenGestor = "token-gestor-mock";

        Usuario usuarioComum = Usuario.builder()
                .id(UUID.randomUUID()).nome("Lucas").email("lucas@email.com")
                .senha("senha-encoded").role(Role.USUARIO).ativo(true).build();

        Usuario gestor = Usuario.builder()
                .id(UUID.randomUUID()).nome("Gestor").email("gestor@email.com")
                .senha("senha-encoded").role(Role.GESTOR).ativo(true).build();

        when(jwtUtil.tokenValido(tokenUsuario)).thenReturn(true);
        when(jwtUtil.tokenValido(tokenGestor)).thenReturn(true);
        when(jwtUtil.extrairEmail(tokenUsuario)).thenReturn(usuarioComum.getEmail());
        when(jwtUtil.extrairEmail(tokenGestor)).thenReturn(gestor.getEmail());
        when(usuarioDetailsService.loadUserByUsername(usuarioComum.getEmail())).thenReturn(usuarioComum);
        when(usuarioDetailsService.loadUserByUsername(gestor.getEmail())).thenReturn(gestor);
    }

    @Test
    @DisplayName("GET /dashboard/admin - deve retornar 401 sem token")
    void obterMetricasAdmin_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/dashboard/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /dashboard/admin - deve retornar 403 pra usuário comum")
    void obterMetricasAdmin_deveRetornar403ParaUsuarioComum() throws Exception {
        mockMvc.perform(get("/v1/dashboard/admin")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /dashboard/admin - deve permitir gestor")
    void obterMetricasAdmin_devePermitirGestor() throws Exception {
        when(dashboardAdminService.obterMetricasAdmin()).thenReturn(DashboardAdminResponseDTO.builder().build());

        mockMvc.perform(get("/v1/dashboard/admin")
                        .header("Authorization", "Bearer " + tokenGestor))
                .andExpect(status().isOk());
    }
}