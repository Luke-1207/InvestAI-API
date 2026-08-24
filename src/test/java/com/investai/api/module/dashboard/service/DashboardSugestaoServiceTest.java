package com.investai.api.module.dashboard.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.ativo.service.CotacaoService;
import com.investai.api.module.dashboard.dto.SugestoesRendaFixaResponseDTO;
import com.investai.api.module.dashboard.dto.SugestoesRendaVariavelResponseDTO;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PreferenciaSetor;
import com.investai.api.module.perfil.entity.SetorPreferido;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.service.RendaFixaUnificadaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardSugestaoServiceTest {

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private CotacaoService cotacaoService;

    @Mock
    private RendaFixaUnificadaService rendaFixaUnificadaService;

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @InjectMocks
    private DashboardSugestaoService dashboardSugestaoService;

    private Acao criarAcao(String codigo, TipoAtivo tipo, String setor) {
        return Acao.builder().id(UUID.randomUUID()).codigo(codigo).nome(codigo).tipo(tipo).setor(setor).ativo(true).build();
    }

    private CotacaoResponseDTO criarCotacao(BigDecimal preco, BigDecimal variacaoDia, BigDecimal dy) {
        return CotacaoResponseDTO.builder().preco(preco).variacaoPercentual(variacaoDia).dividendYield(dy).build();
    }

    private PerfilInvestidor criarPerfil(String risco, String horizonte, String objetivo, BigDecimal valorDisponivel,
                                         List<String> tiposAceitos, List<SetorPreferido> setores, boolean preenchido) {
        return PerfilInvestidor.builder()
                .id(UUID.randomUUID())
                .perfilRisco(risco).horizonte(horizonte).objetivo(objetivo)
                .valorDisponivel(valorDisponivel)
                .tiposAceitos(tiposAceitos == null ? List.of() : tiposAceitos)
                .setoresPreferidos(setores == null ? List.of() : setores)
                .perfilPreenchido(preenchido)
                .build();
    }

    private RendaFixaListagemResponseDTO criarTitulo(CategoriaRendaFixa categoria, String indexador, LocalDate vencimento,
                                                     String liquidez, boolean isentoIr, boolean garantidoFgc, BigDecimal valorMinimo) {
        return RendaFixaListagemResponseDTO.builder()
                .id(UUID.randomUUID()).categoria(categoria).nome(categoria + " teste")
                .indexador(indexador).taxa(BigDecimal.TEN).vencimento(vencimento)
                .valorMinimo(valorMinimo).liquidez(liquidez).isentoIr(isentoIr).garantidoFgc(garantidoFgc)
                .build();
    }

    @Test
    @DisplayName("sugerirRendaVariavel - deve retornar lista vazia com mensagem quando perfil não preenchido")
    void sugerirRendaVariavel_deveRetornarVazioComMensagemQuandoPerfilNaoPreenchido() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(criarPerfil(null, null, null, null, null, null, false)));

        SugestoesRendaVariavelResponseDTO resultado = dashboardSugestaoService.sugerirRendaVariavel(usuarioId);

        assertThat(resultado.getItens()).isEmpty();
        assertThat(resultado.getMensagem()).isEqualTo("Complete seu perfil para receber sugestões personalizadas.");
    }

    @Test
    @DisplayName("sugerirRendaVariavel - deve lançar exceção quando perfil não encontrado")
    void sugerirRendaVariavel_deveLancarExcecaoQuandoPerfilNaoEncontrado() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardSugestaoService.sugerirRendaVariavel(usuarioId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("sugerirRendaVariavel - deve excluir ações fora dos tiposAceitos do perfil")
    void sugerirRendaVariavel_deveExcluirAcoesForaDosTiposAceitos() {
        UUID usuarioId = UUID.randomUUID();
        Acao acao = criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo");
        Acao fii = criarAcao("MXRF11", TipoAtivo.FII, "Papel");

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("MODERADO", "MEDIO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999),
                        List.of("ACAO"), List.of(), true)));
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(acao, fii));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(2), BigDecimal.valueOf(4)));

        SugestoesRendaVariavelResponseDTO resultado = dashboardSugestaoService.sugerirRendaVariavel(usuarioId);

        assertThat(resultado.getItens()).hasSize(1);
        assertThat(resultado.getItens().get(0).getCodigo()).isEqualTo("PETR4");
    }

    @Test
    @DisplayName("sugerirRendaVariavel - deve excluir ação quando cotação está indisponível, sem quebrar")
    void sugerirRendaVariavel_deveExcluirAcaoQuandoCotacaoIndisponivel() {
        UUID usuarioId = UUID.randomUUID();
        Acao comCotacao = criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo");
        Acao semCotacao = criarAcao("XXXX3", TipoAtivo.ACAO, "Desconhecido");

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("MODERADO", "MEDIO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999),
                        List.of(), List.of(), true)));
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(comCotacao, semCotacao));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(2), BigDecimal.valueOf(4)));
        when(cotacaoService.obterCotacao("XXXX3")).thenThrow(new ResourceNotFoundException("sem cotação"));

        SugestoesRendaVariavelResponseDTO resultado = dashboardSugestaoService.sugerirRendaVariavel(usuarioId);

        assertThat(resultado.getItens()).hasSize(1);
        assertThat(resultado.getItens().get(0).getCodigo()).isEqualTo("PETR4");
    }

    @Test
    @DisplayName("sugerirRendaVariavel - conservador com ação de baixa volatilidade deve pontuar mais que arrojado")
    void sugerirRendaVariavel_conservadorComBaixaVolatilidade_devePontuarMais() {
        UUID usuarioId = UUID.randomUUID();
        Acao acao = criarAcao("TAEE3", TipoAtivo.ACAO, "Energia");

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("CONSERVADOR", "MEDIO_PRAZO", "PRESERVAR_CAPITAL", BigDecimal.valueOf(9999),
                        List.of(), List.of(), true)));
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(acao));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(0.5), BigDecimal.valueOf(4)));

        SugestoesRendaVariavelResponseDTO resultado = dashboardSugestaoService.sugerirRendaVariavel(usuarioId);

        assertThat(resultado.getItens().get(0).getScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("sugerirRendaVariavel - conservador com ação de alta volatilidade deve pontuar negativamente esse critério")
    void sugerirRendaVariavel_conservadorComAltaVolatilidade_devePontuarMenos() {
        UUID usuarioId = UUID.randomUUID();
        Acao baixaVol = criarAcao("TAEE3", TipoAtivo.ACAO, "Energia");
        Acao altaVol = criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo");

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("CONSERVADOR", "MEDIO_PRAZO", "PRESERVAR_CAPITAL", BigDecimal.valueOf(9999),
                        List.of(), List.of(), true)));
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(baixaVol, altaVol));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(0.5), BigDecimal.ZERO));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(5), BigDecimal.ZERO));

        SugestoesRendaVariavelResponseDTO resultado = dashboardSugestaoService.sugerirRendaVariavel(usuarioId);

        int scoreBaixaVol = resultado.getItens().stream().filter(i -> i.getCodigo().equals("TAEE3")).findFirst().get().getScore();
        int scoreAltaVol = resultado.getItens().stream().filter(i -> i.getCodigo().equals("PETR4")).findFirst().get().getScore();
        assertThat(scoreBaixaVol).isGreaterThan(scoreAltaVol);
    }

    @Test
    @DisplayName("sugerirRendaVariavel - deve dar bônus de setor preferido e penalizar setor evitado")
    void sugerirRendaVariavel_deveConsiderarSetoresPreferidos() {
        UUID usuarioId = UUID.randomUUID();
        Acao preferido = criarAcao("ITSA4", TipoAtivo.ACAO, "Bancos");
        Acao evitado = criarAcao("VALE3", TipoAtivo.ACAO, "Mineração");

        SetorPreferido pref = SetorPreferido.builder().setor("Bancos").preferencia(PreferenciaSetor.PREFERIR).build();
        SetorPreferido evi = SetorPreferido.builder().setor("Mineração").preferencia(PreferenciaSetor.EVITAR).build();

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("MODERADO", "MEDIO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999),
                        List.of(), List.of(pref, evi), true)));
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(preferido, evitado));
        when(cotacaoService.obterCotacao("ITSA4")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(1.5), BigDecimal.ZERO));
        when(cotacaoService.obterCotacao("VALE3")).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(1.5), BigDecimal.ZERO));

        SugestoesRendaVariavelResponseDTO resultado = dashboardSugestaoService.sugerirRendaVariavel(usuarioId);

        int scorePreferido = resultado.getItens().stream().filter(i -> i.getCodigo().equals("ITSA4")).findFirst().get().getScore();
        int scoreEvitado = resultado.getItens().stream().filter(i -> i.getCodigo().equals("VALE3")).findFirst().get().getScore();
        assertThat(scorePreferido).isGreaterThan(scoreEvitado);
    }

    @Test
    @DisplayName("sugerirRendaVariavel - deve limitar a 5 itens e ordenar por score decrescente")
    void sugerirRendaVariavel_deveLimitarA5EOrdenarPorScore() {
        UUID usuarioId = UUID.randomUUID();
        List<Acao> acoes = List.of(
                criarAcao("A1", TipoAtivo.ACAO, "Setor"), criarAcao("A2", TipoAtivo.ACAO, "Setor"),
                criarAcao("A3", TipoAtivo.ACAO, "Setor"), criarAcao("A4", TipoAtivo.ACAO, "Setor"),
                criarAcao("A5", TipoAtivo.ACAO, "Setor"), criarAcao("A6", TipoAtivo.ACAO, "Setor")
        );

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("MODERADO", "MEDIO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999),
                        List.of(), List.of(), true)));
        when(acaoRepository.findByAtivoTrue()).thenReturn(acoes);
        for (Acao a : acoes) {
            when(cotacaoService.obterCotacao(a.getCodigo())).thenReturn(criarCotacao(BigDecimal.TEN, BigDecimal.valueOf(1.5), BigDecimal.ZERO));
        }

        SugestoesRendaVariavelResponseDTO resultado = dashboardSugestaoService.sugerirRendaVariavel(usuarioId);

        assertThat(resultado.getItens()).hasSize(5);
    }

    @Test
    @DisplayName("sugerirRendaFixa - deve retornar lista vazia com mensagem quando perfil não preenchido")
    void sugerirRendaFixa_deveRetornarVazioComMensagemQuandoPerfilNaoPreenchido() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(criarPerfil(null, null, null, null, null, null, false)));

        SugestoesRendaFixaResponseDTO resultado = dashboardSugestaoService.sugerirRendaFixa(usuarioId);

        assertThat(resultado.getItens()).isEmpty();
        assertThat(resultado.getMensagem()).isEqualTo("Complete seu perfil para receber sugestões personalizadas.");
    }

    @Test
    @DisplayName("sugerirRendaFixa - conservador com SELIC (indexador previsível) deve pontuar mais que com PREFIXADO")
    void sugerirRendaFixa_conservadorComIndexadorPrevisivel_devePontuarMais() {
        UUID usuarioId = UUID.randomUUID();
        RendaFixaListagemResponseDTO selic = criarTitulo(CategoriaRendaFixa.TESOURO, "SELIC",
                LocalDate.now().plusDays(2000), "NO_VENCIMENTO", false, false, BigDecimal.valueOf(9999));
        RendaFixaListagemResponseDTO prefixado = criarTitulo(CategoriaRendaFixa.CDB, "PREFIXADO",
                LocalDate.now().plusDays(2000), "NO_VENCIMENTO", false, false, BigDecimal.valueOf(9999));

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("CONSERVADOR", "LONGO_PRAZO", "PRESERVAR_CAPITAL", BigDecimal.valueOf(1), List.of(), List.of(), true)));
        when(rendaFixaUnificadaService.listar("livre", usuarioId)).thenReturn(List.of(selic, prefixado));

        SugestoesRendaFixaResponseDTO resultado = dashboardSugestaoService.sugerirRendaFixa(usuarioId);

        int scoreSelic = resultado.getItens().stream().filter(i -> i.getCategoria() == CategoriaRendaFixa.TESOURO).findFirst().get().getScore();
        int scorePrefixado = resultado.getItens().stream().filter(i -> i.getCategoria() == CategoriaRendaFixa.CDB).findFirst().get().getScore();
        assertThat(scoreSelic).isGreaterThan(scorePrefixado);
    }

    @Test
    @DisplayName("sugerirRendaFixa - vencimento muito além do horizonte deve penalizar o score")
    void sugerirRendaFixa_vencimentoMuitoAlemDoHorizonte_devePenalizar() {
        UUID usuarioId = UUID.randomUUID();
        RendaFixaListagemResponseDTO dentro = criarTitulo(CategoriaRendaFixa.TESOURO, "SELIC",
                LocalDate.now().plusDays(100), "DIARIA", false, false, BigDecimal.valueOf(9999));
        RendaFixaListagemResponseDTO muitoAlem = criarTitulo(CategoriaRendaFixa.TESOURO, "SELIC",
                LocalDate.now().plusDays(3000), "DIARIA", false, false, BigDecimal.valueOf(9999));

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("MODERADO", "CURTO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(1), List.of(), List.of(), true)));
        when(rendaFixaUnificadaService.listar("livre", usuarioId)).thenReturn(List.of(dentro, muitoAlem));

        SugestoesRendaFixaResponseDTO resultado = dashboardSugestaoService.sugerirRendaFixa(usuarioId);

        assertThat(resultado.getItens().get(0).getVencimento()).isEqualTo(dentro.getVencimento());
    }

    @Test
    @DisplayName("sugerirRendaFixa - deve limitar a 3 itens")
    void sugerirRendaFixa_deveLimitarA3Itens() {
        UUID usuarioId = UUID.randomUUID();
        List<RendaFixaListagemResponseDTO> titulos = List.of(
                criarTitulo(CategoriaRendaFixa.TESOURO, "SELIC", LocalDate.now().plusDays(100), "DIARIA", false, false, BigDecimal.ONE),
                criarTitulo(CategoriaRendaFixa.CDB, "CDI", LocalDate.now().plusDays(100), "DIARIA", false, true, BigDecimal.ONE),
                criarTitulo(CategoriaRendaFixa.LCI, "CDI", LocalDate.now().plusDays(100), "DIARIA", true, true, BigDecimal.ONE),
                criarTitulo(CategoriaRendaFixa.LCA, "CDI", LocalDate.now().plusDays(100), "DIARIA", true, true, BigDecimal.ONE)
        );

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("MODERADO", "CURTO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999), List.of(), List.of(), true)));
        when(rendaFixaUnificadaService.listar("livre", usuarioId)).thenReturn(titulos);

        SugestoesRendaFixaResponseDTO resultado = dashboardSugestaoService.sugerirRendaFixa(usuarioId);

        assertThat(resultado.getItens()).hasSize(3);
    }

    @Test
    @DisplayName("sugerirRendaFixa - score nunca deve ser negativo nem exceder 100")
    void sugerirRendaFixa_scoreDeveEstarSempreEntre0E100() {
        UUID usuarioId = UUID.randomUUID();
        RendaFixaListagemResponseDTO titulo = criarTitulo(CategoriaRendaFixa.TESOURO, "SELIC",
                LocalDate.now().plusDays(100), "DIARIA", true, true, BigDecimal.ONE);

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("CONSERVADOR", "CURTO_PRAZO", "RENDA_PASSIVA", BigDecimal.valueOf(9999), List.of(), List.of(), true)));
        when(rendaFixaUnificadaService.listar("livre", usuarioId)).thenReturn(List.of(titulo));

        SugestoesRendaFixaResponseDTO resultado = dashboardSugestaoService.sugerirRendaFixa(usuarioId);

        int score = resultado.getItens().get(0).getScore();
        assertThat(score).isBetween(0, 100);
    }

    @Test
    @DisplayName("sugerirRendaFixa - deve classificar compatibilidade respeitando os limiares 70/40")
    void sugerirRendaFixa_deveClassificarCompatibilidadeAlta() {
        UUID usuarioId = UUID.randomUUID();
        RendaFixaListagemResponseDTO titulo = criarTitulo(CategoriaRendaFixa.TESOURO, "SELIC",
                LocalDate.now().plusDays(100), "DIARIA", false, true, BigDecimal.ONE);

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(
                criarPerfil("CONSERVADOR", "CURTO_PRAZO", "CRESCIMENTO_PATRIMONIO", BigDecimal.valueOf(9999), List.of(), List.of(), true)));
        when(rendaFixaUnificadaService.listar("livre", usuarioId)).thenReturn(List.of(titulo));

        SugestoesRendaFixaResponseDTO resultado = dashboardSugestaoService.sugerirRendaFixa(usuarioId);

        assertThat(resultado.getItens().get(0).getCompatibilidade()).isEqualTo(Compatibilidade.ALTA);
    }
}