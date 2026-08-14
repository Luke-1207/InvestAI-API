package com.investai.api.module.perfil.service;

import com.investai.api.module.perfil.dto.QuizResponseDTO;
import com.investai.api.module.perfil.entity.*;
import com.investai.api.module.perfil.repository.QuizPerguntaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilQuizServiceTest {

    @Mock
    private QuizPerguntaRepository quizPerguntaRepository;

    @InjectMocks
    private PerfilQuizService perfilQuizService;

    @Test
    @DisplayName("obterQuiz - deve mapear perguntas e opções preservando a ordem e sem expor mapeamento_json")
    void obterQuiz_deveMapearPerguntasEOpcoesPreservandoOrdem() {
        UUID perguntaObjetivoId = UUID.randomUUID();
        UUID perguntaSetoresId = UUID.randomUUID();

        QuizPergunta perguntaObjetivo = QuizPergunta.builder()
                .id(perguntaObjetivoId)
                .ordem(1)
                .texto("O que você quer conquistar com seus investimentos?")
                .tipo(TipoPergunta.UNICA_ESCOLHA)
                .campoPerfil(CampoPerfilQuiz.OBJETIVO_FINANCEIRO)
                .obrigatoria(true)
                .ativa(true)
                .build();
        perguntaObjetivo.setOpcoes(List.of(
                QuizOpcao.builder()
                        .id(UUID.randomUUID())
                        .quizPergunta(perguntaObjetivo)
                        .ordem(1)
                        .texto("Receber uma renda todo mês, sem precisar vender nada")
                        .emoji("💰")
                        .mapeamentoJson(Map.of("objetivo", "RENDA_PASSIVA"))
                        .build(),
                QuizOpcao.builder()
                        .id(UUID.randomUUID())
                        .quizPergunta(perguntaObjetivo)
                        .ordem(2)
                        .texto("Fazer meu dinheiro crescer bastante ao longo do tempo")
                        .emoji("📈")
                        .mapeamentoJson(Map.of("objetivo", "CRESCIMENTO_PATRIMONIO"))
                        .build()
        ));

        QuizPergunta perguntaSetores = QuizPergunta.builder()
                .id(perguntaSetoresId)
                .ordem(6)
                .texto("Tem algum setor que você curte ou prefere evitar?")
                .tipo(TipoPergunta.MULTIPLA_ESCOLHA)
                .campoPerfil(CampoPerfilQuiz.SETORES_PREFERIDOS)
                .obrigatoria(false)
                .ativa(true)
                .build();
        perguntaSetores.setOpcoes(List.of(
                QuizOpcao.builder()
                        .id(UUID.randomUUID())
                        .quizPergunta(perguntaSetores)
                        .ordem(1)
                        .texto("Prefiro não filtrar por setor agora")
                        .emoji("🚫")
                        .mapeamentoJson(Map.of())
                        .build()
        ));

        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc())
                .thenReturn(List.of(perguntaObjetivo, perguntaSetores));

        QuizResponseDTO response = perfilQuizService.obterQuiz();

        assertThat(response.getPerguntas()).hasSize(2);

        assertThat(response.getPerguntas().get(0).getId()).isEqualTo(perguntaObjetivoId);
        assertThat(response.getPerguntas().get(0).getTipo()).isEqualTo(TipoPergunta.UNICA_ESCOLHA);
        assertThat(response.getPerguntas().get(0).isObrigatoria()).isTrue();
        assertThat(response.getPerguntas().get(0).getOpcoes()).hasSize(2);
        assertThat(response.getPerguntas().get(0).getOpcoes().get(0).getTexto())
                .isEqualTo("Receber uma renda todo mês, sem precisar vender nada");
        assertThat(response.getPerguntas().get(0).getOpcoes().get(0).getEmoji()).isEqualTo("💰");

        assertThat(response.getPerguntas().get(1).getId()).isEqualTo(perguntaSetoresId);
        assertThat(response.getPerguntas().get(1).isObrigatoria()).isFalse();
        assertThat(response.getPerguntas().get(1).getOpcoes()).hasSize(1);
    }

    @Test
    @DisplayName("obterQuiz - deve retornar lista vazia quando não há perguntas ativas")
    void obterQuiz_deveRetornarListaVaziaQuandoNaoHaPerguntasAtivas() {
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc())
                .thenReturn(List.of());

        QuizResponseDTO response = perfilQuizService.obterQuiz();

        assertThat(response.getPerguntas()).isEmpty();
    }
}