package com.investai.api.module.dashboard.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.dashboard.dto.IndicadoresMercadoResponseDTO;
import com.investai.api.module.dashboard.service.IndicadoresMercadoSincronizacaoService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IndicadoresMercadoController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class IndicadoresMercadoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IndicadoresMercadoSincronizacaoService indicadoresMercadoSincronizacaoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private static final String BODY = "{ \"selicAtual\": 14.25, \"ipcaAcumulado12m\": 4.83 }";

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
    @DisplayName("PATCH /dashboard/indicadores/selic-ipca - deve retornar 401 sem token")
    void atualizarSelicIpca_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(patch("/v1/dashboard/indicadores/selic-ipca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /dashboard/indicadores/selic-ipca - deve retornar 403 pra usuário comum")
    void atualizarSelicIpca_deveRetornar403ParaUsuarioComum() throws Exception {
        mockMvc.perform(patch("/v1/dashboard/indicadores/selic-ipca")
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /dashboard/indicadores/selic-ipca - deve permitir gestor")
    void atualizarSelicIpca_devePermitirGestor() throws Exception {
        when(indicadoresMercadoSincronizacaoService.atualizarSelicIpcaManualmente(any(), any()))
                .thenReturn(IndicadoresMercadoResponseDTO.builder().build());

        mockMvc.perform(patch("/v1/dashboard/indicadores/selic-ipca")
                        .header("Authorization", "Bearer " + tokenGestor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());
    }
}