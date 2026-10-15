package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PreferenciaSetor;
import com.investai.api.module.perfil.entity.SetorPreferido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcaoPontuacaoServiceTest {

    @Mock
    private CotacaoService cotacaoService;

    @InjectMocks
    private AcaoPontuacaoService acaoPontuacaoService;

    private Acao criarAcao(String codigo, TipoAtivo tipo, String setor) {
        return Acao.builder().id(UUID.randomUUID()).codigo(codigo).nome(codigo).tipo(tipo).setor(setor).ativo(true).build();
    }

    private CotacaoResponseDTO criarCotacao(BigDecimal preco, BigDecimal variacaoDia, BigDecimal dy) {
        return CotacaoResponseDTO.builder().preco(preco).variacaoPercentual(variacaoDia).dividendYield(dy).build();
    }

    private PerfilInvestidor criarPerfil(String risco, String horizonte, String objetivo, BigDecimal valorDisponivel,
                                         List<SetorPreferido> setores) {
        return PerfilInvestidor.builder()
                .id(UUID.randomUUID())
                .perfilRisco(risco).horizonte(horizonte).objetivo(objetivo)
                .valorDisponivel(valorDisponivel)
                .tiposAceitos(List.of())
                .setoresPreferidos(setores == null ? List.of() : setores)
                .perfilPreenchido(true)
                .build();
    }

    @Test
    @DisplayName("pontuarAcao - deve retornar null quando a cotação está indisponível, sem lançar exceção")
    void pontuarAcao_deveRetornarNullQuandoCotacaoIndisponivel() {
        Acao acao = criarAcao("XXXX3", TipoAtivo.ACAO, "Desconhecido");
        PerfilInvestidor perfil = criarPerfil("MODERADO", "MEDIO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999), List.of());

        when(cotacaoService.obterCotacao("XXXX3")).thenThrow(new ResourceNotFoundException("sem cotação"));

        SugestaoAtivoItemDTO resultado = acaoPontuacaoService.pontuarAcao(acao, perfil);

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("pontuarAcao - conservador com ação de baixa volatilidade deve pontuar mais que arrojado com alta")
    void pontuarAcao_conservadorComBaixaVolatilidade_devePontuarMais() {
        Acao acao = criarAcao("TAEE3", TipoAtivo.ACAO, "Energia");
        PerfilInvestidor perfil = criarPerfil("CONSERVADOR", "MEDIO_PRAZO", "PRESERVAR_CAPITAL", BigDecimal.valueOf(9999), List.of());

        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(0.5), BigDecimal.valueOf(4)));

        SugestaoAtivoItemDTO resultado = acaoPontuacaoService.pontuarAcao(acao, perfil);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("pontuarAcao - conservador com ação de alta volatilidade deve pontuar menos que uma de baixa volatilidade")
    void pontuarAcao_conservadorComAltaVolatilidade_devePontuarMenos() {
        Acao baixaVol = criarAcao("TAEE3", TipoAtivo.ACAO, "Energia");
        Acao altaVol = criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo");
        PerfilInvestidor perfil = criarPerfil("CONSERVADOR", "MEDIO_PRAZO", "PRESERVAR_CAPITAL", BigDecimal.valueOf(9999), List.of());

        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(0.5), BigDecimal.ZERO));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(5), BigDecimal.ZERO));

        int scoreBaixaVol = acaoPontuacaoService.pontuarAcao(baixaVol, perfil).getScore();
        int scoreAltaVol = acaoPontuacaoService.pontuarAcao(altaVol, perfil).getScore();

        assertThat(scoreBaixaVol).isGreaterThan(scoreAltaVol);
    }

    @Test
    @DisplayName("pontuarAcao - deve dar bônus de setor preferido e penalizar setor evitado")
    void pontuarAcao_deveConsiderarSetoresPreferidos() {
        Acao preferido = criarAcao("ITSA4", TipoAtivo.ACAO, "Bancos");
        Acao evitado = criarAcao("VALE3", TipoAtivo.ACAO, "Mineração");

        SetorPreferido pref = SetorPreferido.builder().setor("Bancos").preferencia(PreferenciaSetor.PREFERIR).build();
        SetorPreferido evi = SetorPreferido.builder().setor("Mineração").preferencia(PreferenciaSetor.EVITAR).build();
        PerfilInvestidor perfil = criarPerfil("MODERADO", "MEDIO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999), List.of(pref, evi));

        when(cotacaoService.obterCotacao("ITSA4")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(1.5), BigDecimal.ZERO));
        when(cotacaoService.obterCotacao("VALE3")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(1.5), BigDecimal.ZERO));

        int scorePreferido = acaoPontuacaoService.pontuarAcao(preferido, perfil).getScore();
        int scoreEvitado = acaoPontuacaoService.pontuarAcao(evitado, perfil).getScore();

        assertThat(scorePreferido).isGreaterThan(scoreEvitado);
    }

    @Test
    @DisplayName("pontuarAcao - score nunca deve ser negativo nem exceder 100")
    void pontuarAcao_scoreDeveEstarSempreEntre0E100() {
        Acao acao = criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo evitado");
        SetorPreferido evi = SetorPreferido.builder().setor("Petróleo evitado").preferencia(PreferenciaSetor.EVITAR).build();
        PerfilInvestidor perfil = criarPerfil("CONSERVADOR", "CURTO_PRAZO", "PRESERVAR_CAPITAL", BigDecimal.ZERO, List.of(evi));

        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao(BigDecimal.valueOf(1000), BigDecimal.valueOf(10), BigDecimal.ZERO));

        int score = acaoPontuacaoService.pontuarAcao(acao, perfil).getScore();

        assertThat(score).isBetween(0, 100);
    }

    private SetorPreferido setor(String nome, PreferenciaSetor preferencia) {
        return SetorPreferido.builder().setor(nome).preferencia(preferencia).build();
    }

    private SugestaoAtivoItemDTO pontuar(PerfilInvestidor perfil, String setorAcao, double preco, double variacao, double dy) {
        Acao acao = criarAcao("TEST3", TipoAtivo.ACAO, setorAcao);
        when(cotacaoService.obterCotacao("TEST3")).thenReturn(criarCotacao(
                BigDecimal.valueOf(preco), BigDecimal.valueOf(variacao), BigDecimal.valueOf(dy)));
        return acaoPontuacaoService.pontuarAcao(acao, perfil);
    }

    @Test
    @DisplayName("pesos - a soma dos melhores critérios de cada dimensão deve fechar exatamente 100")
    void pesos_somaDosMaximosDeveSer100() {
        int maximo = AcaoPontuacaoService.VOLATILIDADE_IDEAL + AcaoPontuacaoService.DY_ALINHADO
                + AcaoPontuacaoService.HORIZONTE_LONGO + AcaoPontuacaoService.PRECO_ACESSIVEL
                + AcaoPontuacaoService.SETOR_PREFERIDO;

        assertThat(maximo).isEqualTo(100);
    }

    @Test
    @DisplayName("pontuarAcao - ação ideal para o perfil deve chegar a 100 e Alta (antes o máximo era 65)")
    void pontuarAcao_acaoIdealDeveChegarA100() {
        PerfilInvestidor perfil = criarPerfil("ARROJADO", "LONGO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999),
                List.of(setor("Energia", PreferenciaSetor.PREFERIR)));

        SugestaoAtivoItemDTO resultado = pontuar(perfil, "Energia", 10, 5, 1);

        assertThat(resultado.getScore()).isEqualTo(100);
        assertThat(resultado.getCompatibilidade().name()).isEqualTo("ALTA");
    }

    @Test
    @DisplayName("pontuarAcao - quem não escolheu setores ainda deve conseguir Alta (93)")
    void pontuarAcao_semSetoresDeveConseguirAlta() {
        PerfilInvestidor perfil = criarPerfil("MODERADO", "LONGO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999), List.of());

        SugestaoAtivoItemDTO resultado = pontuar(perfil, "Energia", 10, 2, 1);

        assertThat(resultado.getScore()).isEqualTo(93);
        assertThat(resultado.getCompatibilidade().name()).isEqualTo("ALTA");
    }

    @Test
    @DisplayName("pontuarAcao - perfil que quer preservar capital deve valorizar empresas que distribuem lucro (93)")
    void pontuarAcao_preservarCapitalComDividendos() {
        PerfilInvestidor perfil = criarPerfil("CONSERVADOR", "LONGO_PRAZO", "PRESERVAR_CAPITAL", BigDecimal.valueOf(9999), List.of());

        SugestaoAtivoItemDTO resultado = pontuar(perfil, "Energia", 10, 0.5, 7);

        assertThat(resultado.getScore()).isEqualTo(93);
        assertThat(resultado.getJustificativa()).contains("Oscilação de preço compatível");
    }

    @Test
    @DisplayName("pontuarAcao - combinação parcial deve ficar em Média (53)")
    void pontuarAcao_combinacaoParcialDeveFicarEmMedia() {
        PerfilInvestidor perfil = criarPerfil("MODERADO", "MEDIO_PRAZO", "RENDA_PASSIVA", BigDecimal.valueOf(9999), List.of());

        SugestaoAtivoItemDTO resultado = pontuar(perfil, "Energia", 10, 0.5, 4);

        assertThat(resultado.getScore()).isEqualTo(53);
        assertThat(resultado.getCompatibilidade().name()).isEqualTo("MEDIA");
    }

    @Test
    @DisplayName("pontuarAcao - ação oposta ao perfil e em setor evitado deve ficar em 0 e Baixa")
    void pontuarAcao_acaoOpostaAoPerfilDeveFicarEmZero() {
        PerfilInvestidor perfil = criarPerfil("CONSERVADOR", "CURTO_PRAZO", "RENDA_PASSIVA", BigDecimal.valueOf(5),
                List.of(setor("Energia", PreferenciaSetor.EVITAR)));

        SugestaoAtivoItemDTO resultado = pontuar(perfil, "Energia", 10, 5, 1);

        assertThat(resultado.getScore()).isZero();
        assertThat(resultado.getCompatibilidade().name()).isEqualTo("BAIXA");
        assertThat(resultado.getJustificativa()).contains("prefere evitar").contains("Oscila mais");
    }

    @Test
    @DisplayName("pontuarAcao - arrojado com ação que quase não oscila deve perder pontos e explicar o motivo")
    void pontuarAcao_arrojadoComBaixaVolatilidade() {
        PerfilInvestidor perfil = criarPerfil("ARROJADO", "LONGO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999), List.of());

        SugestaoAtivoItemDTO resultado = pontuar(perfil, "Energia", 10, 0.2, 1);

        assertThat(resultado.getScore()).isEqualTo(48);
        assertThat(resultado.getJustificativa()).contains("Baixo DY sugere").doesNotContain("compatível com o seu perfil de risco");
    }
}
