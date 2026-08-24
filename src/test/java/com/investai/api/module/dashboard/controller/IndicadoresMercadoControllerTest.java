package com.investai.api.module.dashboard.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.dashboard.dto.IndicadoresMercadoResponseDTO;
import com.investai.api.module.dashboard.service.IndicadoresMercadoSincronizacaoService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IndicadoresMercadoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class IndicadoresMercadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IndicadoresMercadoSincronizacaoService indicadoresMercadoSincronizacaoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("PATCH /dashboard/indicadores/selic-ipca - deve retornar 200 com o snapshot atualizado")
    void atualizarSelicIpcaManualmente_deveRetornar200() throws Exception {
        when(indicadoresMercadoSincronizacaoService.atualizarSelicIpcaManualmente(any(), any()))
                .thenReturn(IndicadoresMercadoResponseDTO.builder()
                        .selicAtual(BigDecimal.valueOf(14.25))
                        .ipcaAcumulado12m(BigDecimal.valueOf(4.83))
                        .build());

        mockMvc.perform(patch("/v1/dashboard/indicadores/selic-ipca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"selicAtual\": 14.25, \"ipcaAcumulado12m\": 4.83 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selicAtual").value(14.25))
                .andExpect(jsonPath("$.ipcaAcumulado12m").value(4.83));
    }

    @Test
    @DisplayName("PATCH /dashboard/indicadores/selic-ipca - deve retornar 400 quando selicAtual é negativa")
    void atualizarSelicIpcaManualmente_deveRetornar400QuandoSelicNegativa() throws Exception {
        mockMvc.perform(patch("/v1/dashboard/indicadores/selic-ipca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"selicAtual\": -1, \"ipcaAcumulado12m\": 4.83 }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /dashboard/indicadores/selic-ipca - deve retornar 400 quando ipcaAcumulado12m está ausente")
    void atualizarSelicIpcaManualmente_deveRetornar400QuandoIpcaAusente() throws Exception {
        mockMvc.perform(patch("/v1/dashboard/indicadores/selic-ipca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"selicAtual\": 14.25 }"))
                .andExpect(status().isBadRequest());
    }
}