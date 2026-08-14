package com.investai.api.module.perfil.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.perfil.dto.QuizOpcaoResponseDTO;
import com.investai.api.module.perfil.dto.QuizPerguntaResponseDTO;
import com.investai.api.module.perfil.dto.QuizResponseDTO;
import com.investai.api.module.perfil.entity.TipoPergunta;
import com.investai.api.module.perfil.service.PerfilQuizService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}