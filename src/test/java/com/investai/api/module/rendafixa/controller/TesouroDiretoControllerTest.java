package com.investai.api.module.rendafixa.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.rendafixa.dto.TituloTesouroDetalheResponseDTO;
import com.investai.api.module.rendafixa.dto.TituloTesouroListagemResponseDTO;
import com.investai.api.module.rendafixa.dto.ValorDescritoDTO;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.service.TituloTesouroDetalheService;
import com.investai.api.module.rendafixa.service.TituloTesouroListagemService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TesouroDiretoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TesouroDiretoControllerTest {

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
    @DisplayName("GET /renda-fixa/tesouro - deve retornar 200 com página de resultados")
    void listar_deveRetornar200ComPage() throws Exception {
        TituloTesouroListagemResponseDTO dto = TituloTesouroListagemResponseDTO.builder()
                .codigo("tesouro-selic-2029-mock")
                .nome("Tesouro Selic 2029")
                .tipo(TipoTesouro.SELIC)
                .taxaAnual(BigDecimal.valueOf(0.05))
                .precoMinimo(BigDecimal.valueOf(150.00))
                .vencimento(LocalDate.of(2029, 3, 1))
                .pagaJurosSemestrais(false)
                .build();

        when(tituloTesouroListagemService.listar(any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/v1/renda-fixa/tesouro")
                        .param("tipo", "SELIC")
                        .param("taxaMinima", "0.01")
                        .param("ordenarPor", "TAXA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigo").value("tesouro-selic-2029-mock"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /renda-fixa/tesouro/{codigo} - deve retornar 200 com o detalhe")
    void obterDetalhe_deveRetornar200() throws Exception {
        TituloTesouroDetalheResponseDTO dto = TituloTesouroDetalheResponseDTO.builder()
                .codigo("tesouro-selic-2029-mock")
                .nome("Tesouro Selic 2029")
                .tipo(ValorDescritoDTO.builder().valor("SELIC").descricao("...").build())
                .taxaAnual(BigDecimal.valueOf(0.05))
                .precoMinimo(BigDecimal.valueOf(150.00))
                .vencimento(LocalDate.of(2029, 3, 1))
                .pagaJurosSemestrais(false)
                .liquidez("DIARIA")
                .resumoIA(null)
                .build();

        when(tituloTesouroDetalheService.obterDetalhe("tesouro-selic-2029-mock")).thenReturn(dto);

        mockMvc.perform(get("/v1/renda-fixa/tesouro/tesouro-selic-2029-mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("tesouro-selic-2029-mock"))
                .andExpect(jsonPath("$.tipo.valor").value("SELIC"))
                .andExpect(jsonPath("$.liquidez").value("DIARIA"))
                .andExpect(jsonPath("$.resumoIA").doesNotExist());
    }

    @Test
    @DisplayName("GET /renda-fixa/tesouro/{codigo} - deve retornar 404 quando não encontrado")
    void obterDetalhe_deveRetornar404QuandoNaoEncontrado() throws Exception {
        when(tituloTesouroDetalheService.obterDetalhe(anyString()))
                .thenThrow(new ResourceNotFoundException("Título do Tesouro não encontrado: inexistente"));

        mockMvc.perform(get("/v1/renda-fixa/tesouro/inexistente"))
                .andExpect(status().isNotFound());
    }
}