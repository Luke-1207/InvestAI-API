package com.investai.api.module.rendafixa.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.service.RendaFixaUnificadaService;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RendaFixaUnificadaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RendaFixaUnificadaControllerTest {

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
    @DisplayName("GET /renda-fixa - sem modo deve usar 'livre' como padrão")
    void listar_semModo_deveUsarLivreComoPadrao() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());
        when(rendaFixaUnificadaService.listar(eq("livre"), any())).thenReturn(List.of(
                RendaFixaListagemResponseDTO.builder()
                        .id(UUID.randomUUID())
                        .categoria(CategoriaRendaFixa.TESOURO)
                        .nome("Tesouro Selic 2029")
                        .taxa(BigDecimal.valueOf(0.08))
                        .vencimento(LocalDate.now().plusYears(3))
                        .build()
        ));

        mockMvc.perform(get("/v1/renda-fixa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("TESOURO"));
    }

    @Test
    @DisplayName("GET /renda-fixa?modo=inteligente - deve repassar o modo pro service")
    void listar_comModoInteligente_deveRepassarModo() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());
        when(rendaFixaUnificadaService.listar(eq("inteligente"), any())).thenReturn(List.of());

        mockMvc.perform(get("/v1/renda-fixa").param("modo", "inteligente"))
                .andExpect(status().isOk());
    }
}