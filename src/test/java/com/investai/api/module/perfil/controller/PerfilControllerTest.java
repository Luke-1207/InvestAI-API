package com.investai.api.module.perfil.controller;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.perfil.dto.*;
import com.investai.api.module.perfil.entity.TipoPergunta;
import com.investai.api.module.perfil.service.PerfilQuizService;
import com.investai.api.module.perfil.service.PerfilService;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PerfilController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilQuizService perfilQuizService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @MockitoBean
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @MockitoBean
    private PerfilService perfilService;

    @Test
    @DisplayName("GET /perfil - deve retornar 200 com o perfil do usuário")
    void obterPerfil_deveRetornar200() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());

        PerfilResponseDTO response = PerfilResponseDTO.builder()
                .perfilRisco(ValorDescritoDTO.builder().valor("MODERADO").descricao("...").build())
                .valorDisponivel(new BigDecimal("1250.00"))
                .tiposAceitos(List.of(TipoAtivo.ACAO))
                .setoresPreferidos(List.of())
                .perfilPreenchido(true)
                .resumoIA("resumo qualquer")
                .build();

        when(perfilService.obterPerfil(any())).thenReturn(response);

        mockMvc.perform(get("/v1/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilRisco.valor").value("MODERADO"))
                .andExpect(jsonPath("$.valorDisponivel").value(1250.00))
                .andExpect(jsonPath("$.perfilPreenchido").value(true));
    }

    @Test
    @DisplayName("PUT /perfil - deve retornar 200 com o perfil atualizado")
    void editarPerfil_deveRetornar200() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());

        PerfilResponseDTO response = PerfilResponseDTO.builder()
                .perfilRisco(ValorDescritoDTO.builder().valor("ARROJADO").descricao("...").build())
                .valorDisponivel(new BigDecimal("3500.50"))
                .tiposAceitos(List.of(TipoAtivo.ACAO, TipoAtivo.ETF))
                .setoresPreferidos(List.of())
                .perfilPreenchido(true)
                .build();

        when(perfilService.editarPerfil(any(), any())).thenReturn(response);

        String body = """
                {
                  "perfilRisco": "ARROJADO",
                  "horizonteInvestimento": "CURTO_PRAZO",
                  "objetivoFinanceiro": "PRESERVAR_CAPITAL",
                  "valorDisponivel": 3500.50,
                  "tiposAceitos": ["ACAO", "ETF"],
                  "setoresPreferidos": []
                }
                """;

        mockMvc.perform(put("/v1/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilRisco.valor").value("ARROJADO"))
                .andExpect(jsonPath("$.tiposAceitos", org.hamcrest.Matchers.containsInAnyOrder("ACAO", "ETF")));
    }

    @Test
    @DisplayName("PUT /perfil - deve retornar 400 quando perfilRisco não é informado")
    void editarPerfil_deveRetornar400QuandoPerfilRiscoAusente() throws Exception {
        String body = """
                {
                  "horizonteInvestimento": "CURTO_PRAZO",
                  "objetivoFinanceiro": "PRESERVAR_CAPITAL",
                  "valorDisponivel": 3500.50,
                  "tiposAceitos": ["ACAO"],
                  "setoresPreferidos": []
                }
                """;

        mockMvc.perform(put("/v1/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /perfil/quiz - deve retornar estrutura do quiz com sucesso")
    void obterQuiz_deveRetornar200ComEstruturaDoQuiz() throws Exception {
        UUID perguntaId = UUID.randomUUID();
        UUID opcaoId = UUID.randomUUID();

        QuizResponseDTO response = QuizResponseDTO.builder()
                .perguntas(List.of(
                        QuizPerguntaResponseDTO.builder()
                                .id(perguntaId)
                                .texto("O que você quer conquistar com seus investimentos?")
                                .tipo(TipoPergunta.UNICA_ESCOLHA)
                                .obrigatoria(true)
                                .opcoes(List.of(
                                        QuizOpcaoResponseDTO.builder()
                                                .id(opcaoId)
                                                .texto("Receber uma renda todo mês, sem precisar vender nada")
                                                .emoji("💰")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(perfilQuizService.obterQuiz()).thenReturn(response);

        mockMvc.perform(get("/v1/perfil/quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perguntas").isArray())
                .andExpect(jsonPath("$.perguntas[0].id").value(perguntaId.toString()))
                .andExpect(jsonPath("$.perguntas[0].texto").value("O que você quer conquistar com seus investimentos?"))
                .andExpect(jsonPath("$.perguntas[0].tipo").value("UNICA_ESCOLHA"))
                .andExpect(jsonPath("$.perguntas[0].obrigatoria").value(true))
                .andExpect(jsonPath("$.perguntas[0].opcoes[0].id").value(opcaoId.toString()))
                .andExpect(jsonPath("$.perguntas[0].opcoes[0].texto").value("Receber uma renda todo mês, sem precisar vender nada"))
                .andExpect(jsonPath("$.perguntas[0].opcoes[0].emoji").value("💰"))
                .andExpect(jsonPath("$.perguntas[0].campoPerfil").doesNotExist())
                .andExpect(jsonPath("$.perguntas[0].opcoes[0].mapeamentoJson").doesNotExist());
    }

    @Test
    @DisplayName("PUT /perfil/quiz - deve retornar 200 com o perfil calculado")
    void submeterQuiz_deveRetornar200ComPerfilCalculado() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());

        QuizSubmissaoResponseDTO response = QuizSubmissaoResponseDTO.builder()
                .perfilRisco(ValorDescritoDTO.builder().valor("MODERADO").descricao("Você é um investidor moderado...").build())
                .objetivoFinanceiro(ValorDescritoDTO.builder().valor("CRESCIMENTO_PATRIMONIO").descricao("...").build())
                .horizonteInvestimento(ValorDescritoDTO.builder().valor("LONGO_PRAZO").descricao("...").build())
                .resumoIA("Você busca crescimento do patrimônio no longo prazo, com perfil moderado.")
                .build();

        when(perfilQuizService.submeterQuiz(any(), any())).thenReturn(response);

        String body = """
                {
                  "respostas": [
                    { "perguntaId": "%s", "opcaoIds": ["%s"] }
                  ]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(put("/v1/perfil/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilRisco.valor").value("MODERADO"))
                .andExpect(jsonPath("$.objetivoFinanceiro.valor").value("CRESCIMENTO_PATRIMONIO"))
                .andExpect(jsonPath("$.horizonteInvestimento.valor").value("LONGO_PRAZO"))
                .andExpect(jsonPath("$.resumoIA").value("Você busca crescimento do patrimônio no longo prazo, com perfil moderado."));
    }

    @Test
    @DisplayName("PUT /perfil/quiz - deve retornar 400 quando respostas está vazio")
    void submeterQuiz_deveRetornar400QuandoRespostasVazio() throws Exception {
        mockMvc.perform(put("/v1/perfil/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"respostas\": [] }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /perfil/quiz - deve retornar 422 quando o service recusa a submissão")
    void submeterQuiz_deveRetornar422QuandoServiceLancaBusinessException() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());
        when(perfilQuizService.submeterQuiz(any(), any()))
                .thenThrow(new BusinessException("Perguntas obrigatórias não respondidas: ..."));

        String body = """
                {
                  "respostas": [
                    { "perguntaId": "%s", "opcaoIds": ["%s"] }
                  ]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(put("/v1/perfil/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /perfil/refazer-quiz - deve retornar 200 com perfilPreenchido false")
    void refazerQuiz_deveRetornar200ComPerfilPreenchidoFalse() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());

        PerfilResponseDTO response = PerfilResponseDTO.builder()
                .perfilRisco(ValorDescritoDTO.builder().valor("ARROJADO").descricao("...").build())
                .perfilPreenchido(false)
                .build();

        when(perfilService.refazerQuiz(any())).thenReturn(response);

        mockMvc.perform(patch("/v1/perfil/refazer-quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilPreenchido").value(false))
                .andExpect(jsonPath("$.perfilRisco.valor").value("ARROJADO"));
    }
}