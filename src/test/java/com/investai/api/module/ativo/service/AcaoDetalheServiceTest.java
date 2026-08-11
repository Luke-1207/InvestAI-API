package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.*;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcaoDetalheServiceTest {

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private CotacaoService cotacaoService;

    @Mock
    private HistoricoService historicoService;

    @InjectMocks
    private AcaoDetalheService acaoDetalheService;

    @Test
    @DisplayName("obterDetalhe - deve retornar detalhe completo com sucesso")
    void obterDetalhe_deveRetornarDetalheCompletoComSucesso() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao());
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenReturn(criarHistorico("TAEE3", "1A", 30.0, 45.0));

        AcaoDetalheResponseDTO resultado = acaoDetalheService.obterDetalhe("TAEE3", "1A");

        assertThat(resultado.getCodigo()).isEqualTo("TAEE3");
        assertThat(resultado.isCotacaoDisponivel()).isTrue();
        assertThat(resultado.getPreco()).isEqualByComparingTo(BigDecimal.valueOf(38.42));
        assertThat(resultado.getMinimo52Semanas()).isEqualByComparingTo(BigDecimal.valueOf(30.0));
        assertThat(resultado.getMaximo52Semanas()).isEqualByComparingTo(BigDecimal.valueOf(45.0));
        assertThat(resultado.getPontosGrafico()).hasSize(2);
        assertThat(resultado.getPeriodoGrafico()).isEqualTo("1A");
    }

    @Test
    @DisplayName("obterDetalhe - deve manter precoLucro sempre nulo (limitação da fonte de dados)")
    void obterDetalhe_devePrecoLucroSempreNulo() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao());
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenReturn(criarHistorico("TAEE3", "1A", 30.0, 45.0));

        AcaoDetalheResponseDTO resultado = acaoDetalheService.obterDetalhe("TAEE3", "1A");

        assertThat(resultado.getPrecoLucro()).isNull();
    }

    @Test
    @DisplayName("obterDetalhe - deve incluir o glossário completo com os 5 termos")
    void obterDetalhe_deveIncluirGlossarioCompleto() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao());
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenReturn(criarHistorico("TAEE3", "1A", 30.0, 45.0));

        AcaoDetalheResponseDTO resultado = acaoDetalheService.obterDetalhe("TAEE3", "1A");

        assertThat(resultado.getGlossario())
                .containsKeys("DY", "P/L", "P/VP", "Mín/Máx 52 semanas", "Volume");
    }

    @Test
    @DisplayName("obterDetalhe - deve normalizar código para maiúsculo")
    void obterDetalhe_deveNormalizarCodigoParaMaiusculo() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao());
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenReturn(criarHistorico("TAEE3", "1A", 30.0, 45.0));

        acaoDetalheService.obterDetalhe("  taee3  ", "1A");

        verify(acaoRepository).findByCodigoIgnoreCaseAndAtivoTrue("TAEE3");
    }

    @Test
    @DisplayName("obterDetalhe - deve usar 1A como período padrão quando periodoGrafico é nulo")
    void obterDetalhe_deveUsarPeriodoPadraoQuandoNulo() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao());
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenReturn(criarHistorico("TAEE3", "1A", 30.0, 45.0));

        AcaoDetalheResponseDTO resultado = acaoDetalheService.obterDetalhe("TAEE3", null);

        assertThat(resultado.getPeriodoGrafico()).isEqualTo("1A");
        verify(historicoService, times(1)).obterHistorico(anyString(), anyString());
    }

    @Test
    @DisplayName("obterDetalhe - deve chamar histórico apenas uma vez quando período do gráfico já é 1A")
    void obterDetalhe_deveChamarHistoricoUmaVezQuandoPeriodoJaEUmAno() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao());
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenReturn(criarHistorico("TAEE3", "1A", 30.0, 45.0));

        acaoDetalheService.obterDetalhe("TAEE3", "1A");

        verify(historicoService, times(1)).obterHistorico("TAEE3", "1A");
    }

    @Test
    @DisplayName("obterDetalhe - deve chamar histórico duas vezes quando período do gráfico é diferente de 1A")
    void obterDetalhe_deveChamarHistoricoDuasVezesQuandoPeriodoDiferente() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao());
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenReturn(criarHistorico("TAEE3", "1A", 30.0, 45.0));
        when(historicoService.obterHistorico("TAEE3", "1M"))
                .thenReturn(criarHistorico("TAEE3", "1M", 36.0, 40.0));

        AcaoDetalheResponseDTO resultado = acaoDetalheService.obterDetalhe("TAEE3", "1M");

        verify(historicoService).obterHistorico("TAEE3", "1A");
        verify(historicoService).obterHistorico("TAEE3", "1M");
        assertThat(resultado.getPeriodoGrafico()).isEqualTo("1M");
        assertThat(resultado.getMinimo52Semanas()).isEqualByComparingTo(BigDecimal.valueOf(30.0));
        assertThat(resultado.getMaximo52Semanas()).isEqualByComparingTo(BigDecimal.valueOf(45.0));
    }

    @Test
    @DisplayName("obterDetalhe - deve lançar ResourceNotFoundException quando ativo não cadastrado ou inativo")
    void obterDetalhe_deveLancarExcecaoQuandoAtivoNaoEncontrado() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("NAOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acaoDetalheService.obterDetalhe("NAOEXISTE", "1A"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NAOEXISTE");

        verifyNoInteractions(cotacaoService, historicoService);
    }

    @Test
    @DisplayName("obterDetalhe - deve marcar cotacaoDisponivel=false quando cotação indisponível")
    void obterDetalhe_deveMarcarCotacaoIndisponivel() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3"))
                .thenThrow(new ResourceNotFoundException("Nenhuma cotação encontrada"));
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenReturn(criarHistorico("TAEE3", "1A", 30.0, 45.0));

        AcaoDetalheResponseDTO resultado = acaoDetalheService.obterDetalhe("TAEE3", "1A");

        assertThat(resultado.isCotacaoDisponivel()).isFalse();
        assertThat(resultado.getPreco()).isNull();
        assertThat(resultado.getMinimo52Semanas()).isEqualByComparingTo(BigDecimal.valueOf(30.0));
    }

    @Test
    @DisplayName("obterDetalhe - deve retornar pontosGrafico vazio quando histórico anual indisponível")
    void obterDetalhe_deveRetornarPontosVazioQuandoHistoricoAnualIndisponivel() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao());
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenThrow(new ResourceNotFoundException("Nenhum histórico encontrado"));

        AcaoDetalheResponseDTO resultado = acaoDetalheService.obterDetalhe("TAEE3", "1A");

        assertThat(resultado.getPontosGrafico()).isEmpty();
        assertThat(resultado.getMinimo52Semanas()).isNull();
        assertThat(resultado.getMaximo52Semanas()).isNull();
        assertThat(resultado.isCotacaoDisponivel()).isTrue();
    }

    @Test
    @DisplayName("obterDetalhe - deve manter faixa 52 semanas já calculada quando só o histórico do período do gráfico falha")
    void obterDetalhe_deveManterFaixaCalculadaQuandoSoHistoricoDoGraficoFalha() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao());
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenReturn(criarHistorico("TAEE3", "1A", 30.0, 45.0));
        when(historicoService.obterHistorico("TAEE3", "1M"))
                .thenThrow(new ResourceNotFoundException("Falha ao buscar período específico"));

        AcaoDetalheResponseDTO resultado = acaoDetalheService.obterDetalhe("TAEE3", "1M");

        assertThat(resultado.getMinimo52Semanas()).isEqualByComparingTo(BigDecimal.valueOf(30.0));
        assertThat(resultado.getMaximo52Semanas()).isEqualByComparingTo(BigDecimal.valueOf(45.0));
        assertThat(resultado.getPontosGrafico()).isEmpty();
    }

    @Test
    @DisplayName("obterDetalhe - deve degradar graciosamente quando cotação e histórico falham juntos")
    void obterDetalhe_deveDegradarQuandoAmbosFalham() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3")));
        when(cotacaoService.obterCotacao("TAEE3"))
                .thenThrow(new ResourceNotFoundException("Nenhuma cotação encontrada"));
        when(historicoService.obterHistorico("TAEE3", "1A"))
                .thenThrow(new ResourceNotFoundException("Nenhum histórico encontrado"));

        AcaoDetalheResponseDTO resultado = acaoDetalheService.obterDetalhe("TAEE3", "1A");

        assertThat(resultado.getCodigo()).isEqualTo("TAEE3");
        assertThat(resultado.isCotacaoDisponivel()).isFalse();
        assertThat(resultado.getPontosGrafico()).isEmpty();
        assertThat(resultado.getGlossario()).isNotEmpty();
    }

    private Acao criarAcao(String codigo) {
        return Acao.builder()
                .id(UUID.randomUUID())
                .codigo(codigo)
                .nome("Taesa - Transmissão de Energia")
                .tipo(TipoAtivo.ACAO)
                .setor("Energia Elétrica")
                .ativo(true)
                .build();
    }

    private CotacaoResponseDTO criarCotacao() {
        return CotacaoResponseDTO.builder()
                .codigo("TAEE3")
                .preco(BigDecimal.valueOf(38.42))
                .variacaoPercentual(BigDecimal.valueOf(1.25))
                .variacaoPreco(BigDecimal.valueOf(0.48))
                .dividendYield(BigDecimal.valueOf(6.8))
                .precoValorPatrimonial(BigDecimal.valueOf(1.3))
                .volume(24_300_000L)
                .fonte("MOCK")
                .build();
    }

    private HistoricoPrecoResponseDTO criarHistorico(String codigo, String periodo, double minima, double maxima) {
        List<PontoHistoricoDTO> pontos = List.of(
                PontoHistoricoDTO.builder()
                        .data(LocalDate.now().minusDays(1))
                        .abertura(BigDecimal.valueOf(minima))
                        .fechamento(BigDecimal.valueOf(minima + 2))
                        .maxima(BigDecimal.valueOf(minima + 2))
                        .minima(BigDecimal.valueOf(minima))
                        .volume(100_000L)
                        .build(),
                PontoHistoricoDTO.builder()
                        .data(LocalDate.now())
                        .abertura(BigDecimal.valueOf(maxima - 2))
                        .fechamento(BigDecimal.valueOf(maxima))
                        .maxima(BigDecimal.valueOf(maxima))
                        .minima(BigDecimal.valueOf(maxima - 2))
                        .volume(120_000L)
                        .build()
        );

        return HistoricoPrecoResponseDTO.builder()
                .codigo(codigo)
                .periodo(periodo)
                .pontos(pontos)
                .build();
    }
}