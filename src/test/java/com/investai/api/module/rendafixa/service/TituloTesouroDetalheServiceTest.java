package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.rendafixa.dto.TituloTesouroDetalheResponseDTO;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TituloTesouroDetalheServiceTest {

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @Spy
    private CalculadoraRentabilidadeService calculadoraRentabilidadeService;

    @InjectMocks
    private TituloTesouroDetalheService tituloTesouroDetalheService;

    @Test
    @DisplayName("obterDetalhe - deve retornar dados com descrição legível do tipo e liquidez fixa DIARIA")
    void obterDetalhe_deveRetornarDadosComDescricaoDoTipo() {
        TituloTesouro titulo = TituloTesouro.builder()
                .id(UUID.randomUUID())
                .codigo("tesouro-selic-2029-mock")
                .nome("Tesouro Selic 2029")
                .tipo(TipoTesouro.SELIC)
                .taxaAnual(BigDecimal.valueOf(0.05))
                .precoMinimo(BigDecimal.valueOf(150.00))
                .vencimento(LocalDate.now().plusYears(3))
                .pagaJurosSemestrais(false)
                .disponivel(true)
                .build();

        when(tituloTesouroRepository.findByCodigo("tesouro-selic-2029-mock")).thenReturn(Optional.of(titulo));

        TituloTesouroDetalheResponseDTO resultado = tituloTesouroDetalheService.obterDetalhe("tesouro-selic-2029-mock");

        assertThat(resultado.getCodigo()).isEqualTo("tesouro-selic-2029-mock");
        assertThat(resultado.getTipo().getValor()).isEqualTo("SELIC");
        assertThat(resultado.getTipo().getDescricao()).contains("taxa básica de juros");
        assertThat(resultado.getLiquidez()).isEqualTo("DIARIA");
        assertThat(resultado.getResumoIA()).isNull();
        assertThat(resultado.getRentabilidadeEstimada().isTaxaSomadaAoIndexador()).isTrue();
        assertThat(resultado.getRentabilidadeEstimada().getTaxaLiquidaAnual()).isNull();
        assertThat(resultado.getRentabilidadeEstimada().getPercentualRendimentoLiquido()).isEqualByComparingTo("85.0");
    }

    @Test
    @DisplayName("obterDetalhe - prefixado deve trazer a taxa líquida na mesma unidade da taxa bruta")
    void obterDetalhe_prefixado_deveTrazerTaxaLiquida() {
        TituloTesouro titulo = TituloTesouro.builder()
                .id(UUID.randomUUID())
                .codigo("tesouro-prefixado-2031")
                .nome("Tesouro Prefixado 2031")
                .tipo(TipoTesouro.PREFIXADO)
                .taxaAnual(BigDecimal.valueOf(13.0))
                .precoMinimo(BigDecimal.valueOf(35.00))
                .vencimento(LocalDate.now().plusYears(5))
                .pagaJurosSemestrais(false)
                .disponivel(true)
                .build();

        when(tituloTesouroRepository.findByCodigo("tesouro-prefixado-2031")).thenReturn(Optional.of(titulo));

        TituloTesouroDetalheResponseDTO resultado = tituloTesouroDetalheService.obterDetalhe("tesouro-prefixado-2031");

        assertThat(resultado.getRentabilidadeEstimada().isTaxaSomadaAoIndexador()).isFalse();
        assertThat(resultado.getRentabilidadeEstimada().getAliquotaIR()).isEqualByComparingTo("15.0");
        assertThat(resultado.getRentabilidadeEstimada().getTaxaLiquidaAnual()).isEqualByComparingTo("11.05");
    }

    @Test
    @DisplayName("obterDetalhe - deve lançar exceção quando código não existe")
    void obterDetalhe_deveLancarExcecaoQuandoNaoEncontrado() {
        when(tituloTesouroRepository.findByCodigo("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tituloTesouroDetalheService.obterDetalhe("inexistente"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}