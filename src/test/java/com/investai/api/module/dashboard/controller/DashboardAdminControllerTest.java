package com.investai.api.module.dashboard.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.dashboard.dto.DashboardAdminResponseDTO;
import com.investai.api.module.dashboard.service.DashboardAdminService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardAdminService dashboardAdminService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("GET /dashboard/admin - deve retornar 200 com as métricas")
    void obterMetricasAdmin_deveRetornar200() throws Exception {
        when(dashboardAdminService.obterMetricasAdmin()).thenReturn(DashboardAdminResponseDTO.builder()
                .totalUsuarios(100)
                .iaDisponivel(true)
                .distribuicaoRisco(Map.of("CONSERVADOR", 10L))
                .build());

        mockMvc.perform(get("/v1/dashboard/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsuarios").value(100))
                .andExpect(jsonPath("$.iaDisponivel").value(true));
    }
}