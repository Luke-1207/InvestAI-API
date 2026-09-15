package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.dashboard.dto.SugestoesRendaVariavelResponseDTO;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcaoSugestaoServiceTest {

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private AcaoPontuacaoService acaoPontuacaoService;

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @InjectMocks
    private AcaoSugestaoService acaoSugestaoService;

    private Acao criarAcao(String codigo, TipoAtivo tipo) {
        return Acao.builder().id(UUID.randomUUID()).codigo(codigo).nome(codigo).tipo(tipo).setor("Setor").ativo(true).build();
    }

    private PerfilInvestidor criarPerfil(List<String> tiposAceitos, boolean preenchido) {
        return PerfilInvestidor.builder()
                .id(UUID.randomUUID())
                .perfilRisco("MODERADO").horizonte("MEDIO_PRAZO").objetivo("CRESCIMENTO_PATRIMONIO")
                .valorDisponivel(BigDecimal.valueOf(9999))
                .tiposAceitos(tiposAceitos == null ? List.of() : tiposAceitos)
                .setoresPreferidos(List.of())
                .perfilPreenchido(preenchido)
                .build();
    }

    private SugestaoAtivoItemDTO criarSugestao(String codigo, int score) {
        return SugestaoAtivoItemDTO.builder()
                .codigo(codigo).nome(codigo).tipo(TipoAtivo.ACAO).setor("Setor")
                .preco(BigDecimal.TEN).variacaoDia(BigDecimal.ONE).dy(BigDecimal.ZERO)
                .score(score).compatibilidade(Compatibilidade.MEDIA).justificativa("teste")
                .build();
    }

    @Test
    @DisplayName("deve lançar exceção quando perfil não encontrado")
    void listarSugestoes_deveLancarExcecaoQuandoPerfilNaoEncontrado() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acaoSugestaoService.listarSugestoes(usuarioId, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deve retornar lista vazia com mensagem quando perfil não preenchido, sem nem consultar ações")
    void listarSugestoes_deveRetornarVazioComMensagemQuandoPerfilNaoPreenchido() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(criarPerfil(null, false)));

        SugestoesRendaVariavelResponseDTO resultado = acaoSugestaoService.listarSugestoes(usuarioId, null);

        assertThat(resultado.getItens()).isEmpty();
        assertThat(resultado.getMensagem()).isEqualTo("Complete seu perfil para receber sugestões personalizadas.");
        verifyNoInteractions(acaoRepository);
    }

    @Test
    @DisplayName("deve excluir ativos fora dos tiposAceitos do perfil")
    void listarSugestoes_deveRespeitarTiposAceitosDoPerfil() {
        UUID usuarioId = UUID.randomUUID();
        Acao acao = criarAcao("PETR4", TipoAtivo.ACAO);
        Acao fii = criarAcao("MXRF11", TipoAtivo.FII);
        PerfilInvestidor perfil = criarPerfil(List.of("ACAO"), true);

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(acao, fii));
        when(acaoPontuacaoService.pontuarAcao(acao, perfil)).thenReturn(criarSugestao("PETR4", 60));

        SugestoesRendaVariavelResponseDTO resultado = acaoSugestaoService.listarSugestoes(usuarioId, null);

        assertThat(resultado.getItens()).hasSize(1);
        assertThat(resultado.getItens().get(0).getCodigo()).isEqualTo("PETR4");
    }

    @Test
    @DisplayName("deve aplicar também o filtro de tipo vindo da query (tab Tudo/Ações/FIIs/ETFs)")
    void listarSugestoes_deveAplicarFiltroDeTipoDaQuery() {
        UUID usuarioId = UUID.randomUUID();
        Acao acao = criarAcao("PETR4", TipoAtivo.ACAO);
        Acao fii = criarAcao("MXRF11", TipoAtivo.FII);
        PerfilInvestidor perfil = criarPerfil(List.of("ACAO", "FII"), true);

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(acao, fii));
        when(acaoPontuacaoService.pontuarAcao(fii, perfil)).thenReturn(criarSugestao("MXRF11", 50));

        SugestoesRendaVariavelResponseDTO resultado = acaoSugestaoService.listarSugestoes(usuarioId, List.of(TipoAtivo.FII));

        assertThat(resultado.getItens()).hasSize(1);
        assertThat(resultado.getItens().get(0).getCodigo()).isEqualTo("MXRF11");
    }

    @Test
    @DisplayName("deve descartar itens que o AcaoPontuacaoService não conseguiu pontuar (cotação indisponível)")
    void listarSugestoes_deveDescartarItensNulos() {
        UUID usuarioId = UUID.randomUUID();
        Acao comCotacao = criarAcao("PETR4", TipoAtivo.ACAO);
        Acao semCotacao = criarAcao("XXXX3", TipoAtivo.ACAO);
        PerfilInvestidor perfil = criarPerfil(List.of(), true);

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(comCotacao, semCotacao));
        when(acaoPontuacaoService.pontuarAcao(comCotacao, perfil)).thenReturn(criarSugestao("PETR4", 50));
        when(acaoPontuacaoService.pontuarAcao(semCotacao, perfil)).thenReturn(null);

        SugestoesRendaVariavelResponseDTO resultado = acaoSugestaoService.listarSugestoes(usuarioId, null);

        assertThat(resultado.getItens()).hasSize(1);
    }

    @Test
    @DisplayName("deve ordenar por score decrescente e NÃO limitar a quantidade de itens (diferente do Dashboard)")
    void listarSugestoes_deveOrdenarPorScoreSemLimitarQuantidade() {
        UUID usuarioId = UUID.randomUUID();
        List<Acao> acoes = List.of(
                criarAcao("A1", TipoAtivo.ACAO), criarAcao("A2", TipoAtivo.ACAO),
                criarAcao("A3", TipoAtivo.ACAO), criarAcao("A4", TipoAtivo.ACAO),
                criarAcao("A5", TipoAtivo.ACAO), criarAcao("A6", TipoAtivo.ACAO)
        );
        int[] scores = {10, 90, 30, 80, 50, 20};
        PerfilInvestidor perfil = criarPerfil(List.of(), true);

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(acaoRepository.findByAtivoTrue()).thenReturn(acoes);
        for (int i = 0; i < acoes.size(); i++) {
            when(acaoPontuacaoService.pontuarAcao(acoes.get(i), perfil))
                    .thenReturn(criarSugestao(acoes.get(i).getCodigo(), scores[i]));
        }

        SugestoesRendaVariavelResponseDTO resultado = acaoSugestaoService.listarSugestoes(usuarioId, null);

        assertThat(resultado.getItens()).hasSize(6);
        assertThat(resultado.getItens().get(0).getCodigo()).isEqualTo("A2");
        assertThat(resultado.getItens().get(5).getCodigo()).isEqualTo("A1");
    }
}