package com.investai.api.module.perfil.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.perfil.dto.QuizResponseDTO;
import com.investai.api.module.perfil.dto.QuizSubmissaoResponseDTO;
import com.investai.api.module.perfil.dto.RespostaQuizDTO;
import com.investai.api.module.perfil.dto.SubmeterQuizRequestDTO;
import com.investai.api.module.perfil.entity.*;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.perfil.repository.QuizPerguntaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilQuizServiceTest {

    @Mock
    private QuizPerguntaRepository quizPerguntaRepository;

    @InjectMocks
    private PerfilQuizService perfilQuizService;

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @Mock
    private ResumoPerfilService resumoPerfilService;

    private static final UUID PERGUNTA_OBJETIVO_ID = UUID.randomUUID();
    private static final UUID OPCAO_OBJETIVO_RENDA_ID = UUID.randomUUID();
    private static final UUID OPCAO_OBJETIVO_CRESCIMENTO_ID = UUID.randomUUID();

    private static final UUID PERGUNTA_HORIZONTE_ID = UUID.randomUUID();
    private static final UUID OPCAO_HORIZONTE_LONGO_ID = UUID.randomUUID();

    private static final UUID PERGUNTA_RISCO_ID = UUID.randomUUID();
    private static final UUID OPCAO_RISCO_MODERADO_ID = UUID.randomUUID();

    private static final UUID PERGUNTA_VALOR_ID = UUID.randomUUID();
    private static final UUID OPCAO_VALOR_FAIXA_500_2000_ID = UUID.randomUUID();
    private static final UUID OPCAO_VALOR_SEM_TETO_ID = UUID.randomUUID();

    private static final UUID PERGUNTA_TIPOS_ID = UUID.randomUUID();
    private static final UUID OPCAO_TIPO_ACAO_ID = UUID.randomUUID();
    private static final UUID OPCAO_TIPO_FII_ID = UUID.randomUUID();

    private static final UUID PERGUNTA_SETORES_ID = UUID.randomUUID();
    private static final UUID OPCAO_SETOR_TECNOLOGIA_ID = UUID.randomUUID();
    private static final UUID OPCAO_SETOR_NAO_FILTRAR_ID = UUID.randomUUID();

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

    @Test
    @DisplayName("submeterQuiz - deve mapear respostas para enums e salvar o perfil com sucesso")
    void submeterQuiz_deveMapearRespostasESalvarPerfilComSucesso() {
        UUID usuarioId = UUID.randomUUID();
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc()).thenReturn(criarPerguntasCompletas());
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilVazioMock()));
        when(perfilInvestidorRepository.save(any(PerfilInvestidor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumoPerfilService.gerarResumoIA(any())).thenReturn("Você busca crescimento do patrimônio no longo prazo, com perfil moderado.");

        SubmeterQuizRequestDTO dto = montarRequestDTO(
                resposta(PERGUNTA_OBJETIVO_ID, OPCAO_OBJETIVO_CRESCIMENTO_ID),
                resposta(PERGUNTA_HORIZONTE_ID, OPCAO_HORIZONTE_LONGO_ID),
                resposta(PERGUNTA_RISCO_ID, OPCAO_RISCO_MODERADO_ID),
                resposta(PERGUNTA_VALOR_ID, OPCAO_VALOR_FAIXA_500_2000_ID),
                respostaMultipla(PERGUNTA_TIPOS_ID, OPCAO_TIPO_ACAO_ID, OPCAO_TIPO_FII_ID),
                resposta(PERGUNTA_SETORES_ID, OPCAO_SETOR_TECNOLOGIA_ID)
        );

        QuizSubmissaoResponseDTO response = perfilQuizService.submeterQuiz(usuarioId, dto);

        assertThat(response.getObjetivoFinanceiro().getValor()).isEqualTo("CRESCIMENTO_PATRIMONIO");
        assertThat(response.getHorizonteInvestimento().getValor()).isEqualTo("LONGO_PRAZO");
        assertThat(response.getPerfilRisco().getValor()).isEqualTo("MODERADO");
        assertThat(response.getResumoIA()).isEqualTo("Você busca crescimento do patrimônio no longo prazo, com perfil moderado.");

        ArgumentCaptor<PerfilInvestidor> captor = ArgumentCaptor.forClass(PerfilInvestidor.class);
        verify(perfilInvestidorRepository).save(captor.capture());
        PerfilInvestidor salvo = captor.getValue();
        verify(resumoPerfilService).gerarResumoIA(salvo);

        assertThat(salvo.getObjetivo()).isEqualTo("CRESCIMENTO_PATRIMONIO");
        assertThat(salvo.getHorizonte()).isEqualTo("LONGO_PRAZO");
        assertThat(salvo.getPerfilRisco()).isEqualTo("MODERADO");
        assertThat(salvo.getValorDisponivel()).isEqualByComparingTo("1250.00");
        assertThat(salvo.getTiposAceitos()).containsExactlyInAnyOrder("ACAO", "FII");
        assertThat(salvo.getSetoresPreferidos()).hasSize(1);
        assertThat(salvo.getSetoresPreferidos().get(0).getSetor()).isEqualTo("Tecnologia");
        assertThat(salvo.getSetoresPreferidos().get(0).getPreferencia()).isEqualTo(PreferenciaSetor.PREFERIR);
        assertThat(salvo.isPerfilPreenchido()).isTrue();
    }

    @Test
    @DisplayName("submeterQuiz - deve usar valor fixo quando a faixa de valor disponível não tem teto")
    void submeterQuiz_deveUsarValorFixoQuandoFaixaSemTeto() {
        UUID usuarioId = UUID.randomUUID();
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc()).thenReturn(criarPerguntasCompletas());
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilVazioMock()));
        when(perfilInvestidorRepository.save(any(PerfilInvestidor.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmeterQuizRequestDTO dto = montarRequestDTO(
                resposta(PERGUNTA_OBJETIVO_ID, OPCAO_OBJETIVO_CRESCIMENTO_ID),
                resposta(PERGUNTA_HORIZONTE_ID, OPCAO_HORIZONTE_LONGO_ID),
                resposta(PERGUNTA_RISCO_ID, OPCAO_RISCO_MODERADO_ID),
                resposta(PERGUNTA_VALOR_ID, OPCAO_VALOR_SEM_TETO_ID),
                respostaMultipla(PERGUNTA_TIPOS_ID, OPCAO_TIPO_ACAO_ID)
        );

        perfilQuizService.submeterQuiz(usuarioId, dto);

        ArgumentCaptor<PerfilInvestidor> captor = ArgumentCaptor.forClass(PerfilInvestidor.class);
        verify(perfilInvestidorRepository).save(captor.capture());
        assertThat(captor.getValue().getValorDisponivel()).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("submeterQuiz - opção 'prefiro não filtrar' não deve adicionar setor preferido")
    void submeterQuiz_naoDeveAdicionarSetorQuandoOpcaoForNaoFiltrar() {
        UUID usuarioId = UUID.randomUUID();
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc()).thenReturn(criarPerguntasCompletas());
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilVazioMock()));
        when(perfilInvestidorRepository.save(any(PerfilInvestidor.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmeterQuizRequestDTO dto = montarRequestDTO(
                resposta(PERGUNTA_OBJETIVO_ID, OPCAO_OBJETIVO_CRESCIMENTO_ID),
                resposta(PERGUNTA_HORIZONTE_ID, OPCAO_HORIZONTE_LONGO_ID),
                resposta(PERGUNTA_RISCO_ID, OPCAO_RISCO_MODERADO_ID),
                resposta(PERGUNTA_VALOR_ID, OPCAO_VALOR_FAIXA_500_2000_ID),
                respostaMultipla(PERGUNTA_TIPOS_ID, OPCAO_TIPO_ACAO_ID),
                resposta(PERGUNTA_SETORES_ID, OPCAO_SETOR_NAO_FILTRAR_ID)
        );

        perfilQuizService.submeterQuiz(usuarioId, dto);

        ArgumentCaptor<PerfilInvestidor> captor = ArgumentCaptor.forClass(PerfilInvestidor.class);
        verify(perfilInvestidorRepository).save(captor.capture());
        assertThat(captor.getValue().getSetoresPreferidos()).isEmpty();
    }

    @Test
    @DisplayName("submeterQuiz - deve permitir omitir a pergunta opcional de setores")
    void submeterQuiz_devePermitirOmitirPerguntaOpcional() {
        UUID usuarioId = UUID.randomUUID();
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc()).thenReturn(criarPerguntasCompletas());
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilVazioMock()));
        when(perfilInvestidorRepository.save(any(PerfilInvestidor.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmeterQuizRequestDTO dto = montarRequestDTO(
                resposta(PERGUNTA_OBJETIVO_ID, OPCAO_OBJETIVO_CRESCIMENTO_ID),
                resposta(PERGUNTA_HORIZONTE_ID, OPCAO_HORIZONTE_LONGO_ID),
                resposta(PERGUNTA_RISCO_ID, OPCAO_RISCO_MODERADO_ID),
                resposta(PERGUNTA_VALOR_ID, OPCAO_VALOR_FAIXA_500_2000_ID),
                respostaMultipla(PERGUNTA_TIPOS_ID, OPCAO_TIPO_ACAO_ID)
        );

        QuizSubmissaoResponseDTO response = perfilQuizService.submeterQuiz(usuarioId, dto);

        assertThat(response).isNotNull();
        verify(perfilInvestidorRepository).save(any(PerfilInvestidor.class));
    }

    @Test
    @DisplayName("submeterQuiz - deve lançar exceção quando pergunta obrigatória não é respondida")
    void submeterQuiz_deveLancarExcecaoQuandoPerguntaObrigatoriaNaoRespondida() {
        UUID usuarioId = UUID.randomUUID();
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc()).thenReturn(criarPerguntasCompletas());

        SubmeterQuizRequestDTO dto = montarRequestDTO(
                resposta(PERGUNTA_HORIZONTE_ID, OPCAO_HORIZONTE_LONGO_ID),
                resposta(PERGUNTA_RISCO_ID, OPCAO_RISCO_MODERADO_ID),
                resposta(PERGUNTA_VALOR_ID, OPCAO_VALOR_FAIXA_500_2000_ID),
                respostaMultipla(PERGUNTA_TIPOS_ID, OPCAO_TIPO_ACAO_ID)
        );

        assertThatThrownBy(() -> perfilQuizService.submeterQuiz(usuarioId, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Perguntas obrigatórias não respondidas")
                .hasMessageContaining("O que você quer conquistar com seus investimentos?");

        verify(perfilInvestidorRepository, never()).save(any());
    }

    @Test
    @DisplayName("submeterQuiz - deve lançar exceção quando perguntaId não existe entre as perguntas ativas")
    void submeterQuiz_deveLancarExcecaoQuandoPerguntaIdInvalido() {
        UUID usuarioId = UUID.randomUUID();
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc()).thenReturn(criarPerguntasCompletas());

        SubmeterQuizRequestDTO dto = montarRequestDTO(
                resposta(UUID.randomUUID(), UUID.randomUUID()),
                resposta(PERGUNTA_HORIZONTE_ID, OPCAO_HORIZONTE_LONGO_ID),
                resposta(PERGUNTA_RISCO_ID, OPCAO_RISCO_MODERADO_ID),
                resposta(PERGUNTA_VALOR_ID, OPCAO_VALOR_FAIXA_500_2000_ID),
                respostaMultipla(PERGUNTA_TIPOS_ID, OPCAO_TIPO_ACAO_ID),
                resposta(PERGUNTA_OBJETIVO_ID, OPCAO_OBJETIVO_CRESCIMENTO_ID)
        );

        assertThatThrownBy(() -> perfilQuizService.submeterQuiz(usuarioId, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pergunta inválida ou inativa");

        verify(perfilInvestidorRepository, never()).save(any());
    }

    @Test
    @DisplayName("submeterQuiz - deve lançar exceção quando pergunta de única escolha recebe mais de uma opção")
    void submeterQuiz_deveLancarExcecaoQuandoUnicaEscolhaRecebeMultiplasOpcoes() {
        UUID usuarioId = UUID.randomUUID();
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc()).thenReturn(criarPerguntasCompletas());

        SubmeterQuizRequestDTO dto = montarRequestDTO(
                respostaMultipla(PERGUNTA_OBJETIVO_ID, OPCAO_OBJETIVO_RENDA_ID, OPCAO_OBJETIVO_CRESCIMENTO_ID),
                resposta(PERGUNTA_HORIZONTE_ID, OPCAO_HORIZONTE_LONGO_ID),
                resposta(PERGUNTA_RISCO_ID, OPCAO_RISCO_MODERADO_ID),
                resposta(PERGUNTA_VALOR_ID, OPCAO_VALOR_FAIXA_500_2000_ID),
                respostaMultipla(PERGUNTA_TIPOS_ID, OPCAO_TIPO_ACAO_ID)
        );

        assertThatThrownBy(() -> perfilQuizService.submeterQuiz(usuarioId, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("aceita apenas uma opção");

        verify(perfilInvestidorRepository, never()).save(any());
    }

    @Test
    @DisplayName("submeterQuiz - deve lançar exceção quando opção não pertence à pergunta informada")
    void submeterQuiz_deveLancarExcecaoQuandoOpcaoNaoPertenceAPergunta() {
        UUID usuarioId = UUID.randomUUID();
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc()).thenReturn(criarPerguntasCompletas());

        SubmeterQuizRequestDTO dto = montarRequestDTO(
                resposta(PERGUNTA_OBJETIVO_ID, OPCAO_HORIZONTE_LONGO_ID), // opção de outra pergunta
                resposta(PERGUNTA_HORIZONTE_ID, OPCAO_HORIZONTE_LONGO_ID),
                resposta(PERGUNTA_RISCO_ID, OPCAO_RISCO_MODERADO_ID),
                resposta(PERGUNTA_VALOR_ID, OPCAO_VALOR_FAIXA_500_2000_ID),
                respostaMultipla(PERGUNTA_TIPOS_ID, OPCAO_TIPO_ACAO_ID)
        );

        assertThatThrownBy(() -> perfilQuizService.submeterQuiz(usuarioId, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Opção inválida para a pergunta");

        verify(perfilInvestidorRepository, never()).save(any());
    }

    @Test
    @DisplayName("submeterQuiz - deve lançar exceção quando perfil do investidor não é encontrado")
    void submeterQuiz_deveLancarExcecaoQuandoPerfilNaoEncontrado() {
        UUID usuarioId = UUID.randomUUID();
        when(quizPerguntaRepository.findAllByAtivaTrueOrderByOrdemAsc()).thenReturn(criarPerguntasCompletas());
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        SubmeterQuizRequestDTO dto = montarRequestDTO(
                resposta(PERGUNTA_OBJETIVO_ID, OPCAO_OBJETIVO_CRESCIMENTO_ID),
                resposta(PERGUNTA_HORIZONTE_ID, OPCAO_HORIZONTE_LONGO_ID),
                resposta(PERGUNTA_RISCO_ID, OPCAO_RISCO_MODERADO_ID),
                resposta(PERGUNTA_VALOR_ID, OPCAO_VALOR_FAIXA_500_2000_ID),
                respostaMultipla(PERGUNTA_TIPOS_ID, OPCAO_TIPO_ACAO_ID)
        );

        assertThatThrownBy(() -> perfilQuizService.submeterQuiz(usuarioId, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Perfil do investidor não encontrado");

        verify(perfilInvestidorRepository, never()).save(any());
    }

    private List<QuizPergunta> criarPerguntasCompletas() {
        QuizPergunta perguntaObjetivo = QuizPergunta.builder()
                .id(PERGUNTA_OBJETIVO_ID).ordem(1)
                .texto("O que você quer conquistar com seus investimentos?")
                .tipo(TipoPergunta.UNICA_ESCOLHA).campoPerfil(CampoPerfilQuiz.OBJETIVO_FINANCEIRO)
                .obrigatoria(true).ativa(true).build();
        perguntaObjetivo.setOpcoes(List.of(
                criarOpcao(perguntaObjetivo, OPCAO_OBJETIVO_RENDA_ID, "Receber renda", Map.of("objetivo", "RENDA_PASSIVA")),
                criarOpcao(perguntaObjetivo, OPCAO_OBJETIVO_CRESCIMENTO_ID, "Crescer patrimônio", Map.of("objetivo", "CRESCIMENTO_PATRIMONIO"))
        ));

        QuizPergunta perguntaHorizonte = QuizPergunta.builder()
                .id(PERGUNTA_HORIZONTE_ID).ordem(2)
                .texto("Em quanto tempo você pensa em usar esse dinheiro?")
                .tipo(TipoPergunta.UNICA_ESCOLHA).campoPerfil(CampoPerfilQuiz.HORIZONTE_INVESTIMENTO)
                .obrigatoria(true).ativa(true).build();
        perguntaHorizonte.setOpcoes(List.of(
                criarOpcao(perguntaHorizonte, OPCAO_HORIZONTE_LONGO_ID, "Mais de 5 anos", Map.of("horizonte", "LONGO_PRAZO"))
        ));

        QuizPergunta perguntaRisco = QuizPergunta.builder()
                .id(PERGUNTA_RISCO_ID).ordem(3)
                .texto("Se seu investimento caísse 20% de repente, o que você faria?")
                .tipo(TipoPergunta.UNICA_ESCOLHA).campoPerfil(CampoPerfilQuiz.PERFIL_RISCO)
                .obrigatoria(true).ativa(true).build();
        perguntaRisco.setOpcoes(List.of(
                criarOpcao(perguntaRisco, OPCAO_RISCO_MODERADO_ID, "Ficaria preocupado, mas esperaria", Map.of("perfilRisco", "MODERADO"))
        ));

        QuizPergunta perguntaValor = QuizPergunta.builder()
                .id(PERGUNTA_VALOR_ID).ordem(4)
                .texto("Quanto você tem disponível para começar a investir agora?")
                .tipo(TipoPergunta.UNICA_ESCOLHA).campoPerfil(CampoPerfilQuiz.VALOR_DISPONIVEL)
                .obrigatoria(true).ativa(true).build();
        perguntaValor.setOpcoes(List.of(
                criarOpcao(perguntaValor, OPCAO_VALOR_FAIXA_500_2000_ID, "Entre R$ 500 e R$ 2.000",
                        mapaValorDisponivel(500, 2000)),
                criarOpcao(perguntaValor, OPCAO_VALOR_SEM_TETO_ID, "Mais de R$ 10.000",
                        mapaValorDisponivel(10000, null))
        ));

        QuizPergunta perguntaTipos = QuizPergunta.builder()
                .id(PERGUNTA_TIPOS_ID).ordem(5)
                .texto("Que tipos de investimento você toparia explorar?")
                .tipo(TipoPergunta.MULTIPLA_ESCOLHA).campoPerfil(CampoPerfilQuiz.TIPOS_ACEITOS)
                .obrigatoria(true).ativa(true).build();
        perguntaTipos.setOpcoes(List.of(
                criarOpcao(perguntaTipos, OPCAO_TIPO_ACAO_ID, "Ações", Map.of("tiposAceitos", List.of("ACAO"))),
                criarOpcao(perguntaTipos, OPCAO_TIPO_FII_ID, "FIIs", Map.of("tiposAceitos", List.of("FII")))
        ));

        QuizPergunta perguntaSetores = QuizPergunta.builder()
                .id(PERGUNTA_SETORES_ID).ordem(6)
                .texto("Tem algum setor que você curte ou prefere evitar?")
                .tipo(TipoPergunta.MULTIPLA_ESCOLHA).campoPerfil(CampoPerfilQuiz.SETORES_PREFERIDOS)
                .obrigatoria(false).ativa(true).build();
        perguntaSetores.setOpcoes(List.of(
                criarOpcao(perguntaSetores, OPCAO_SETOR_TECNOLOGIA_ID, "Tecnologia",
                        Map.of("setor", "Tecnologia", "preferencia", "PREFERIR")),
                criarOpcao(perguntaSetores, OPCAO_SETOR_NAO_FILTRAR_ID, "Prefiro não filtrar por setor agora", Map.of())
        ));

        return List.of(perguntaObjetivo, perguntaHorizonte, perguntaRisco, perguntaValor, perguntaTipos, perguntaSetores);
    }

    private QuizOpcao criarOpcao(QuizPergunta pergunta, UUID id, String texto, Map<String, Object> mapeamento) {
        return QuizOpcao.builder().id(id).quizPergunta(pergunta).ordem(1).texto(texto).mapeamentoJson(mapeamento).build();
    }

    private Map<String, Object> mapaValorDisponivel(Integer min, Integer max) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("valorDisponivelMin", min);
        mapa.put("valorDisponivelMax", max);
        return mapa;
    }

    private RespostaQuizDTO resposta(UUID perguntaId, UUID opcaoId) {
        RespostaQuizDTO r = new RespostaQuizDTO();
        r.setPerguntaId(perguntaId);
        r.setOpcaoIds(List.of(opcaoId));
        return r;
    }

    private RespostaQuizDTO respostaMultipla(UUID perguntaId, UUID... opcaoIds) {
        RespostaQuizDTO r = new RespostaQuizDTO();
        r.setPerguntaId(perguntaId);
        r.setOpcaoIds(List.of(opcaoIds));
        return r;
    }

    private SubmeterQuizRequestDTO montarRequestDTO(RespostaQuizDTO... respostas) {
        SubmeterQuizRequestDTO dto = new SubmeterQuizRequestDTO();
        dto.setRespostas(List.of(respostas));
        return dto;
    }

    private PerfilInvestidor perfilVazioMock() {
        return PerfilInvestidor.builder().id(UUID.randomUUID()).perfilPreenchido(false).build();
    }
}