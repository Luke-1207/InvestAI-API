package com.investai.api.module.perfil.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.perfil.dto.*;
import com.investai.api.module.perfil.entity.*;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.perfil.repository.QuizPerguntaRepository;
import com.investai.api.shared.event.PerfilAlteradoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfilQuizService {

    private final QuizPerguntaRepository quizPerguntaRepository;
    private final PerfilInvestidorRepository perfilInvestidorRepository;
    private final ResumoPerfilService resumoPerfilService;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final BigDecimal VALOR_DISPONIVEL_FAIXA_ABERTA = BigDecimal.valueOf(15000);

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

    @Transactional
    public QuizSubmissaoResponseDTO submeterQuiz(UUID usuarioId, SubmeterQuizRequestDTO dto) {
        List<QuizPergunta> perguntasAtivas = quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc();
        Map<UUID, QuizPergunta> perguntasPorId = perguntasAtivas.stream()
                .collect(Collectors.toMap(QuizPergunta::getId, p -> p));

        validarPerguntasObrigatoriasRespondidas(perguntasAtivas, dto.getRespostas());

        PerfilAcumulado acumulado = new PerfilAcumulado();

        for (RespostaQuizDTO resposta : dto.getRespostas()) {
            QuizPergunta pergunta = perguntasPorId.get(resposta.getPerguntaId());
            if (pergunta == null) {
                throw new BusinessException("Pergunta inválida ou inativa: " + resposta.getPerguntaId());
            }

            if (pergunta.getTipo() == TipoPergunta.UNICA_ESCOLHA && resposta.getOpcaoIds().size() != 1) {
                throw new BusinessException("A pergunta \"" + pergunta.getTexto() + "\" aceita apenas uma opção");
            }

            for (UUID opcaoId : resposta.getOpcaoIds()) {
                QuizOpcao opcao = pergunta.getOpcoes().stream()
                        .filter(o -> o.getId().equals(opcaoId))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(
                                "Opção inválida para a pergunta \"" + pergunta.getTexto() + "\""));

                aplicarMapeamento(acumulado, pergunta.getCampoPerfil(), opcao.getMapeamentoJson());
            }
        }

        PerfilInvestidor perfil = perfilInvestidorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil do investidor não encontrado"));

        perfil.setPerfilRisco(acumulado.perfilRisco.name());
        perfil.setObjetivo(acumulado.objetivo.name());
        perfil.setHorizonte(acumulado.horizonte.name());
        perfil.setValorDisponivel(acumulado.valorDisponivel);
        perfil.setTiposAceitos(new ArrayList<>(acumulado.tiposAceitos));
        perfil.setSetoresPreferidos(acumulado.setoresPreferidos);
        perfil.setPerfilPreenchido(true);

        perfilInvestidorRepository.save(perfil);
        applicationEventPublisher.publishEvent(new PerfilAlteradoEvent(this, usuarioId));
        return montarResposta(acumulado, perfil);
    }

    private void validarPerguntasObrigatoriasRespondidas(List<QuizPergunta> perguntasAtivas, List<RespostaQuizDTO> respostas) {
        Set<UUID> perguntasRespondidas = respostas.stream()
                .map(RespostaQuizDTO::getPerguntaId)
                .collect(Collectors.toSet());

        List<String> faltando = perguntasAtivas.stream()
                .filter(QuizPergunta::isObrigatoria)
                .filter(p -> !perguntasRespondidas.contains(p.getId()))
                .map(QuizPergunta::getTexto)
                .toList();

        if (!faltando.isEmpty()) {
            throw new BusinessException("Perguntas obrigatórias não respondidas: " + String.join("; ", faltando));
        }
    }

    @SuppressWarnings("unchecked")
    private void aplicarMapeamento(PerfilAcumulado acumulado, CampoPerfilQuiz campoPerfil, Map<String, Object> mapeamento) {
        switch (campoPerfil) {
            case OBJETIVO_FINANCEIRO -> acumulado.objetivo = ObjetivoFinanceiro.valueOf((String) mapeamento.get("objetivo"));
            case HORIZONTE_INVESTIMENTO -> acumulado.horizonte = HorizonteInvestimento.valueOf((String) mapeamento.get("horizonte"));
            case PERFIL_RISCO -> acumulado.perfilRisco = PerfilRisco.valueOf((String) mapeamento.get("perfilRisco"));
            case VALOR_DISPONIVEL -> acumulado.valorDisponivel = calcularValorDisponivel(mapeamento);
            case TIPOS_ACEITOS -> acumulado.tiposAceitos.addAll((List<String>) mapeamento.get("tiposAceitos"));
            case SETORES_PREFERIDOS -> {
                Object setor = mapeamento.get("setor");
                if (setor != null) {
                    acumulado.setoresPreferidos.add(SetorPreferido.builder()
                            .setor((String) setor)
                            .preferencia(PreferenciaSetor.valueOf((String) mapeamento.get("preferencia")))
                            .build());
                }
            }
        }
    }

    private BigDecimal calcularValorDisponivel(Map<String, Object> mapeamento) {
        BigDecimal min = new BigDecimal(mapeamento.get("valorDisponivelMin").toString());
        Object maxRaw = mapeamento.get("valorDisponivelMax");

        if (maxRaw == null) {
            return VALOR_DISPONIVEL_FAIXA_ABERTA;
        }

        BigDecimal max = new BigDecimal(maxRaw.toString());
        return min.add(max).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private QuizSubmissaoResponseDTO montarResposta(PerfilAcumulado acumulado, PerfilInvestidor perfil) {
        return QuizSubmissaoResponseDTO.builder()
                .perfilRisco(ValorDescritoDTO.builder()
                        .valor(acumulado.perfilRisco.name())
                        .descricao(DescricoesPerfil.PERFIL_RISCO.get(acumulado.perfilRisco))
                        .build())
                .objetivoFinanceiro(ValorDescritoDTO.builder()
                        .valor(acumulado.objetivo.name())
                        .descricao(DescricoesPerfil.OBJETIVO_FINANCEIRO.get(acumulado.objetivo))
                        .build())
                .horizonteInvestimento(ValorDescritoDTO.builder()
                        .valor(acumulado.horizonte.name())
                        .descricao(DescricoesPerfil.HORIZONTE_INVESTIMENTO.get(acumulado.horizonte))
                        .build())
                .resumoIA(resumoPerfilService.gerarResumoIA(perfil))
                .build();
    }

    private static class PerfilAcumulado {
        ObjetivoFinanceiro objetivo;
        HorizonteInvestimento horizonte;
        PerfilRisco perfilRisco;
        BigDecimal valorDisponivel;
        Set<String> tiposAceitos = new LinkedHashSet<>();
        List<SetorPreferido> setoresPreferidos = new ArrayList<>();
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