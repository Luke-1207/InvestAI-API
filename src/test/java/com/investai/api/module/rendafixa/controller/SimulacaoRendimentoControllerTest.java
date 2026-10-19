package com.investai.api.module.rendafixa.controller;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.rendafixa.dto.SimulacaoRendimentoRequestDTO;
import com.investai.api.module.rendafixa.dto.SimulacaoRendimentoResponseDTO;
import com.investai.api.module.rendafixa.service.SimuladorRendimentoService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimulacaoRendimentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SimulacaoRendimentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimuladorRendimentoService simuladorRendimentoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private SimulacaoRendimentoResponseDTO resposta() {
        return SimulacaoRendimentoResponseDTO.builder()
                .valorInvestido(new BigDecimal("1000.00")).prazoMeses(12)
                .valorBruto(new BigDecimal("1163.90")).valorLiquido(new BigDecimal("1135.22"))
                .valorPoupanca(new BigDecimal("1061.68"))
                .premissas(List.of("Selic atual de 15,00% ao ano.")).avisos(List.of())
                .build();
    }

    @Test
    @DisplayName("GET /renda-fixa/titulos/{id}/simulacao - deve repassar valor e prazo e devolver 200")
    void simularTituloPrivado_deveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(simuladorRendimentoService.simularTituloPrivado(eq(id), argThat((SimulacaoRendimentoRequestDTO r) ->
                r.getValor().compareTo(new BigDecimal("1000")) == 0 && r.getPrazoMeses() == 12)))
                .thenReturn(resposta());

        mockMvc.perform(get("/v1/renda-fixa/titulos/{id}/simulacao", id)
                        .param("valor", "1000")
                        .param("prazoMeses", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorLiquido").value(1135.22))
                .andExpect(jsonPath("$.valorPoupanca").value(1061.68))
                .andExpect(jsonPath("$.premissas[0]").value("Selic atual de 15,00% ao ano."));
    }

    @Test
    @DisplayName("GET /renda-fixa/tesouro/{codigo}/simulacao - deve devolver 200")
    void simularTesouro_deveRetornar200() throws Exception {
        when(simuladorRendimentoService.simularTesouro(eq("IPCA2035"), any())).thenReturn(resposta());

        mockMvc.perform(get("/v1/renda-fixa/tesouro/IPCA2035/simulacao")
                        .param("valor", "1000")
                        .param("prazoMeses", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorBruto").value(1163.90));
    }

    @Test
    @DisplayName("GET simulacao - sem valor e com prazo zero deve devolver 400 com a mensagem de cada campo")
    void simular_parametrosInvalidos_deveRetornar400() throws Exception {
        mockMvc.perform(get("/v1/renda-fixa/tesouro/IPCA2035/simulacao")
                        .param("prazoMeses", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.valor").value("Informe o valor que você quer investir."))
                .andExpect(jsonPath("$.detalhes.prazoMeses").value("O prazo precisa ser de pelo menos 1 mês."));

        verifyNoInteractions(simuladorRendimentoService);
    }

    @Test
    @DisplayName("GET simulacao - regra de negócio violada deve devolver 422 com a mensagem do serviço")
    void simular_regraDeNegocio_deveRetornar422() throws Exception {
        when(simuladorRendimentoService.simularTesouro(eq("IPCA2035"), any()))
                .thenThrow(new BusinessException("O prazo passa do vencimento do título, em 15/05/2035. Escolha até 102 meses."));

        mockMvc.perform(get("/v1/renda-fixa/tesouro/IPCA2035/simulacao")
                        .param("valor", "1000")
                        .param("prazoMeses", "200"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("O prazo passa do vencimento do título, em 15/05/2035. Escolha até 102 meses."));
    }
}
