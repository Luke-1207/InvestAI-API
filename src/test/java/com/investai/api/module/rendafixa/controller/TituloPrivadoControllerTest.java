package com.investai.api.module.rendafixa.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.rendafixa.dto.TituloPrivadoResponseDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.service.TituloPrivadoService;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TituloPrivadoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TituloPrivadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TituloPrivadoService tituloPrivadoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private TituloPrivadoResponseDTO respostaPadrao(UUID id, boolean ativo) {
        return TituloPrivadoResponseDTO.builder()
                .id(id)
                .tipo(TipoTituloPrivado.CDB)
                .emissor("Banco Inter")
                .indexador(Indexador.CDI)
                .taxaPercentual(BigDecimal.valueOf(112.0))
                .vencimento(LocalDate.now().plusYears(2))
                .investimentoMinimo(BigDecimal.valueOf(500.00))
                .liquidez(TipoLiquidez.DIARIA)
                .garantidoFgc(true)
                .isentoIr(false)
                .ativo(ativo)
                .build();
    }

    @Test
    @DisplayName("POST /renda-fixa/titulos - deve retornar 201 ao cadastrar")
    void cadastrar_deveRetornar201() throws Exception {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoService.cadastrar(any())).thenReturn(respostaPadrao(id, true));

        String body = """
                {
                  "tipo": "CDB",
                  "emissor": "Banco Inter",
                  "indexador": "CDI",
                  "taxaPercentual": 112.0,
                  "vencimento": "%s",
                  "investimentoMinimo": 500.00,
                  "liquidez": "DIARIA",
                  "garantidoFgc": true,
                  "isentoIr": false
                }
                """.formatted(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/v1/renda-fixa/titulos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emissor").value("Banco Inter"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("POST /renda-fixa/titulos - deve retornar 400 quando vencimento está no passado")
    void cadastrar_deveRetornar400QuandoVencimentoNoPassado() throws Exception {
        String body = """
                {
                  "tipo": "CDB",
                  "emissor": "Banco Inter",
                  "indexador": "CDI",
                  "taxaPercentual": 112.0,
                  "vencimento": "2020-01-01",
                  "investimentoMinimo": 500.00,
                  "liquidez": "DIARIA",
                  "garantidoFgc": true,
                  "isentoIr": false
                }
                """;

        mockMvc.perform(post("/v1/renda-fixa/titulos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /renda-fixa/titulos - deve retornar 400 quando taxa é negativa")
    void cadastrar_deveRetornar400QuandoTaxaNegativa() throws Exception {
        String body = """
                {
                  "tipo": "CDB",
                  "emissor": "Banco Inter",
                  "indexador": "CDI",
                  "taxaPercentual": -5.0,
                  "vencimento": "%s",
                  "investimentoMinimo": 500.00,
                  "liquidez": "DIARIA",
                  "garantidoFgc": true,
                  "isentoIr": false
                }
                """.formatted(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/v1/renda-fixa/titulos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /renda-fixa/titulos - deve retornar 400 quando investimentoMinimo está ausente")
    void cadastrar_deveRetornar400QuandoInvestimentoMinimoAusente() throws Exception {
        String body = """
                {
                  "tipo": "CDB",
                  "emissor": "Banco Inter",
                  "indexador": "CDI",
                  "taxaPercentual": 112.0,
                  "vencimento": "%s",
                  "liquidez": "DIARIA",
                  "garantidoFgc": true,
                  "isentoIr": false
                }
                """.formatted(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/v1/renda-fixa/titulos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /renda-fixa/titulos/{id}/status - deve retornar 200 com status alterado")
    void alterarStatus_deveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoService.alterarStatus(eq(id), eq(false))).thenReturn(respostaPadrao(id, false));

        mockMvc.perform(patch("/v1/renda-fixa/titulos/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ativo\": false }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    @DisplayName("DELETE /renda-fixa/titulos/{id} - deve retornar 204")
    void desativar_deveRetornar204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/v1/renda-fixa/titulos/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /renda-fixa/titulos/{id} - deve retornar 404 quando não encontrado")
    void desativar_deveRetornar404QuandoNaoEncontrado() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Título privado não encontrado"))
                .when(tituloPrivadoService).desativar(id);

        mockMvc.perform(delete("/v1/renda-fixa/titulos/{id}", id))
                .andExpect(status().isNotFound());
    }
}