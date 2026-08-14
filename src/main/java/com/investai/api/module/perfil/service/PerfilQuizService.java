package com.investai.api.module.perfil.service;

import com.investai.api.module.perfil.dto.QuizOpcaoResponseDTO;
import com.investai.api.module.perfil.dto.QuizPerguntaResponseDTO;
import com.investai.api.module.perfil.dto.QuizResponseDTO;
import com.investai.api.module.perfil.entity.QuizOpcao;
import com.investai.api.module.perfil.entity.QuizPergunta;
import com.investai.api.module.perfil.repository.QuizPerguntaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PerfilQuizService {

    private final QuizPerguntaRepository quizPerguntaRepository;

    @Transactional(readOnly = true)
    public QuizResponseDTO obterQuiz() {
        List<QuizPerguntaResponseDTO> perguntas = quizPerguntaRepository
                .findAllByAtivaTrueOrderByOrdemAsc()
                .stream()
                .map(this::toPerguntaResponseDTO)
                .toList();

        return QuizResponseDTO.builder()
                .perguntas(perguntas)
                .build();
    }

    private QuizPerguntaResponseDTO toPerguntaResponseDTO(QuizPergunta pergunta) {
        List<QuizOpcaoResponseDTO> opcoes = pergunta.getOpcoes().stream()
                .map(this::toOpcaoResponseDTO)
                .toList();

        return QuizPerguntaResponseDTO.builder()
                .id(pergunta.getId())
                .texto(pergunta.getTexto())
                .tipo(pergunta.getTipo())
                .obrigatoria(pergunta.isObrigatoria())
                .opcoes(opcoes)
                .build();
    }

    private QuizOpcaoResponseDTO toOpcaoResponseDTO(QuizOpcao opcao) {
        return QuizOpcaoResponseDTO.builder()
                .id(opcao.getId())
                .texto(opcao.getTexto())
                .emoji(opcao.getEmoji())
                .build();
    }
}