package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.AcaoListagemResponseDTO;
import com.investai.api.module.ativo.dto.ComparacaoResponseDTO;
import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComparacaoServiceTest {

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private CotacaoService cotacaoService;

    private ComparacaoService comparacaoService;

    @BeforeEach
    void setUp() {
        Executor executorSincrono = Runnable::run;
        comparacaoService = new ComparacaoService(acaoRepository, cotacaoService, executorSincrono);
    }

    @Test
    @DisplayName("comparar - deve retornar comparação com sucesso para dois ativos")
    void comparar_deveRetornarComparacaoComSucesso() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3", "Taesa")));
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("PETR4"))
                .thenReturn(Optional.of(criarAcao("PETR4", "Petrobras")));

        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao("PETR4", 38.90, 14.2));

        ComparacaoResponseDTO resultado = comparacaoService.comparar(List.of("TAEE3", "PETR4"));

        assertThat(resultado.getAtivos()).hasSize(2);
        assertThat(resultado.getAtivos())
                .extracting(AcaoListagemResponseDTO::getCodigo)
                .containsExactlyInAnyOrder("TAEE3", "PETR4");
    }

    @Test
    @DisplayName("comparar - deve normalizar códigos em minúsculo para maiúsculo")
    void comparar_deveNormalizarCodigosParaMaiusculo() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3", "Taesa")));
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("PETR4"))
                .thenReturn(Optional.of(criarAcao("PETR4", "Petrobras")));

        when(cotacaoService.obterCotacao(anyString())).thenReturn(criarCotacao("X", 10.0, 1.0));

        comparacaoService.comparar(List.of("taee3", "petr4"));

        verify(acaoRepository).findByCodigoIgnoreCaseAndAtivoTrue("TAEE3");
        verify(acaoRepository).findByCodigoIgnoreCaseAndAtivoTrue("PETR4");
    }

    @Test
    @DisplayName("comparar - deve deduplicar tickers repetidos na lista")
    void comparar_deveDeduplicarTickersRepetidos() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3", "Taesa")));
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("PETR4"))
                .thenReturn(Optional.of(criarAcao("PETR4", "Petrobras")));

        when(cotacaoService.obterCotacao(anyString())).thenReturn(criarCotacao("X", 10.0, 1.0));

        ComparacaoResponseDTO resultado = comparacaoService.comparar(List.of("TAEE3", "TAEE3", "PETR4"));

        assertThat(resultado.getAtivos()).hasSize(2);
        verify(acaoRepository, times(1)).findByCodigoIgnoreCaseAndAtivoTrue("TAEE3");
    }

    @Test
    @DisplayName("comparar - deve lançar exceção quando menos de dois tickers informados")
    void comparar_deveLancarExcecaoQuandoMenosDeDoisTickers() {
        assertThatThrownBy(() -> comparacaoService.comparar(List.of("TAEE3")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pelo menos 2");

        verifyNoInteractions(acaoRepository);
    }

    @Test
    @DisplayName("comparar - deve lançar exceção quando mais de cinco tickers informados")
    void comparar_deveLancarExcecaoQuandoMaisDeCincoTickers() {
        List<String> seisTickers = List.of("A1", "B2", "C3", "D4", "E5", "F6");

        assertThatThrownBy(() -> comparacaoService.comparar(seisTickers))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no máximo 5");

        verifyNoInteractions(acaoRepository);
    }

    @Test
    @DisplayName("comparar - deve permitir exatamente cinco tickers (limite superior)")
    void comparar_devePermitirExatamenteCincoTickers() {
        List<String> cincoTickers = List.of("A1", "B2", "C3", "D4", "E5");

        cincoTickers.forEach(ticker ->
                when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue(ticker))
                        .thenReturn(Optional.of(criarAcao(ticker, ticker + " Empresa")))
        );
        when(cotacaoService.obterCotacao(anyString())).thenReturn(criarCotacao("X", 10.0, 1.0));

        ComparacaoResponseDTO resultado = comparacaoService.comparar(cincoTickers);

        assertThat(resultado.getAtivos()).hasSize(5);
    }

    @Test
    @DisplayName("comparar - deve lançar ResourceNotFoundException quando ativo não cadastrado")
    void comparar_deveLancarResourceNotFoundQuandoAtivoNaoCadastrado() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3", "Taesa")));
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("NAOEXISTE"))
                .thenReturn(Optional.empty());

        lenient().when(cotacaoService.obterCotacao(anyString())).thenReturn(criarCotacao("X", 10.0, 1.0));

        assertThatThrownBy(() -> comparacaoService.comparar(List.of("TAEE3", "NAOEXISTE")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NAOEXISTE");
    }

    @Test
    @DisplayName("comparar - deve lançar ResourceNotFoundException quando ativo está desativado")
    void comparar_deveLancarResourceNotFoundQuandoAtivoDesativado() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3", "Taesa")));
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("PETR4"))
                .thenReturn(Optional.empty());

        lenient().when(cotacaoService.obterCotacao(anyString())).thenReturn(criarCotacao("X", 10.0, 1.0));

        assertThatThrownBy(() -> comparacaoService.comparar(List.of("TAEE3", "PETR4")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("comparar - deve marcar cotacaoDisponivel=false quando cotação não encontrada mas ativo existe")
    void comparar_deveMarcarCotacaoIndisponivelQuandoCotacaoNaoEncontrada() {
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("TAEE3"))
                .thenReturn(Optional.of(criarAcao("TAEE3", "Taesa")));
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("NOVA3"))
                .thenReturn(Optional.of(criarAcao("NOVA3", "Nova Empresa")));

        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8));
        when(cotacaoService.obterCotacao("NOVA3"))
                .thenThrow(new ResourceNotFoundException("Nenhuma cotação encontrada"));

        ComparacaoResponseDTO resultado = comparacaoService.comparar(List.of("TAEE3", "NOVA3"));

        AcaoListagemResponseDTO nova3 = resultado.getAtivos().stream()
                .filter(dto -> dto.getCodigo().equals("NOVA3"))
                .findFirst()
                .orElseThrow();

        assertThat(nova3.isCotacaoDisponivel()).isFalse();
        assertThat(nova3.getPreco()).isNull();
    }

    private Acao criarAcao(String codigo, String nome) {
        return Acao.builder()
                .id(UUID.randomUUID())
                .codigo(codigo)
                .nome(nome)
                .tipo(TipoAtivo.ACAO)
                .setor("Setor Teste")
                .ativo(true)
                .build();
    }

    private CotacaoResponseDTO criarCotacao(String codigo, double preco, double dy) {
        return CotacaoResponseDTO.builder()
                .codigo(codigo)
                .preco(BigDecimal.valueOf(preco))
                .dividendYield(BigDecimal.valueOf(dy))
                .fonte("MOCK")
                .build();
    }
}