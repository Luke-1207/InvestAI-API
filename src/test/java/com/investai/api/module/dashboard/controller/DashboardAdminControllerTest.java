package com.investai.api.module.dashboard.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.dashboard.dto.DashboardAdminResponseDTO;
import com.investai.api.module.dashboard.dto.StatusIaResponseDTO;
import com.investai.api.module.dashboard.service.DashboardAdminService;
import com.investai.api.module.integracao.dto.SaudeIntegracoesResponseDTO;
import com.investai.api.module.integracao.dto.SituacaoIntegracao;
import com.investai.api.module.integracao.dto.StatusIntegracaoDTO;
import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.service.SaudeIntegracoesService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SaudeIntegracoesService saudeIntegracoesService;

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

    @Test
    @DisplayName("GET /dashboard/admin/ia-status - deve retornar 200 com o status da IA")
    void obterStatusIa_deveRetornar200() throws Exception {
        when(dashboardAdminService.obterStatusIa()).thenReturn(StatusIaResponseDTO.builder()
                .disponivel(true)
                .rabbitmqConectado(true)
                .build());

        mockMvc.perform(get("/v1/dashboard/admin/ia-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponivel").value(true))
                .andExpect(jsonPath("$.rabbitmqConectado").value(true));
    }

    @Test
    @DisplayName("GET /dashboard/admin/integracoes - deve retornar o estado de cada integração")
    void obterSaudeIntegracoes_deveRetornar200() throws Exception {
        when(saudeIntegracoesService.verificar()).thenReturn(SaudeIntegracoesResponseDTO.builder()
                .verificadoEm(LocalDateTime.of(2026, 10, 21, 19, 0))
                .integracoes(List.of(StatusIntegracaoDTO.builder()
                        .chave(Integracao.HG_BRASIL).nome("HG Brasil").situacao(SituacaoIntegracao.OPERACIONAL)
                        .simulado(true).ultimoSucessoEm(LocalDateTime.of(2026, 10, 21, 18, 45)).build()))
                .build());

        mockMvc.perform(get("/v1/dashboard/admin/integracoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integracoes[0].chave").value("HG_BRASIL"))
                .andExpect(jsonPath("$.integracoes[0].situacao").value("OPERACIONAL"))
                .andExpect(jsonPath("$.integracoes[0].simulado").value(true))
                .andExpect(jsonPath("$.integracoes[0].ultimoSucessoEm").value("2026-10-21T18:45:00"));
    }

    @Test
    @DisplayName("POST /dashboard/admin/integracoes/verificar - deve verificar todas na hora")
    void verificarIntegracoes_deveRetornar200() throws Exception {
        when(saudeIntegracoesService.verificarAgora()).thenReturn(SaudeIntegracoesResponseDTO.builder()
                .verificadoEm(LocalDateTime.of(2026, 10, 21, 19, 0))
                .integracoes(List.of(StatusIntegracaoDTO.builder()
                        .chave(Integracao.LLM).nome("LLM (Groq)").situacao(SituacaoIntegracao.OPERACIONAL).build()))
                .build());

        mockMvc.perform(post("/v1/dashboard/admin/integracoes/verificar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integracoes[0].chave").value("LLM"))
                .andExpect(jsonPath("$.verificadoEm").value("2026-10-21T19:00:00"));
    }

    @Test
    @DisplayName("POST /dashboard/admin/integracoes/{chave}/verificar - deve verificar só a integração pedida")
    void verificarIntegracao_deveRetornar200() throws Exception {
        when(saudeIntegracoesService.verificarAgora(Integracao.BANCO_CENTRAL)).thenReturn(StatusIntegracaoDTO.builder()
                .chave(Integracao.BANCO_CENTRAL).nome("Banco Central").situacao(SituacaoIntegracao.FORA_DO_AR)
                .ultimoErro("Tempo esgotado").build());

        mockMvc.perform(post("/v1/dashboard/admin/integracoes/BANCO_CENTRAL/verificar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chave").value("BANCO_CENTRAL"))
                .andExpect(jsonPath("$.situacao").value("FORA_DO_AR"))
                .andExpect(jsonPath("$.ultimoErro").value("Tempo esgotado"));
    }

    @Test
    @DisplayName("POST /dashboard/admin/integracoes/{chave}/verificar - chave desconhecida deve retornar 400")
    void verificarIntegracao_chaveInvalida_deveRetornar400() throws Exception {
        mockMvc.perform(post("/v1/dashboard/admin/integracoes/NAO_EXISTE/verificar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());

        verifyNoInteractions(saudeIntegracoesService);
    }
}
