package com.investai.api.module.rendafixa.service;

import com.investai.api.module.rendafixa.dto.OrdenarPorTesouro;
import com.investai.api.module.rendafixa.dto.TituloTesouroListagemFiltroDTO;
import com.investai.api.module.rendafixa.dto.TituloTesouroListagemResponseDTO;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TituloTesouroListagemServiceTest {

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @InjectMocks
    private TituloTesouroListagemService tituloTesouroListagemService;

    private TituloTesouro criarTitulo(String codigo, TipoTesouro tipo, BigDecimal taxa, BigDecimal precoMinimo, LocalDate vencimento) {
        return TituloTesouro.builder()
                .id(UUID.randomUUID())
                .codigo(codigo)
                .nome(codigo)
                .tipo(tipo)
                .taxaAnual(taxa)
                .precoMinimo(precoMinimo)
                .vencimento(vencimento)
                .pagaJurosSemestrais(false)
                .disponivel(true)
                .build();
    }

    @Test
    @DisplayName("listar - sem filtros deve retornar todos ordenados por vencimento (mais próximo primeiro)")
    void listar_semFiltros_deveRetornarTodosOrdenadosPorVencimento() {
        TituloTesouro selic2031 = criarTitulo("SELIC-2031", TipoTesouro.SELIC, BigDecimal.valueOf(0.08), BigDecimal.valueOf(150), LocalDate.of(2031, 1, 1));
        TituloTesouro ipca2029 = criarTitulo("IPCA-2029", TipoTesouro.IPCA, BigDecimal.valueOf(6.0), BigDecimal.valueOf(90), LocalDate.of(2029, 1, 1));

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(selic2031, ipca2029));

        Page<TituloTesouroListagemResponseDTO> resultado = tituloTesouroListagemService.listar(new TituloTesouroListagemFiltroDTO());

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("IPCA-2029"); // vence antes
        assertThat(resultado.getContent().get(1).getCodigo()).isEqualTo("SELIC-2031");
    }

    @Test
    @DisplayName("listar - deve filtrar por tipo")
    void listar_comFiltroTipo_deveRetornarSoDoTipo() {
        TituloTesouro selic = criarTitulo("SELIC-1", TipoTesouro.SELIC, BigDecimal.valueOf(0.08), BigDecimal.valueOf(150), LocalDate.of(2031, 1, 1));
        TituloTesouro ipca = criarTitulo("IPCA-1", TipoTesouro.IPCA, BigDecimal.valueOf(6.0), BigDecimal.valueOf(90), LocalDate.of(2029, 1, 1));

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(selic, ipca));

        TituloTesouroListagemFiltroDTO filtro = new TituloTesouroListagemFiltroDTO();
        filtro.setTipo(TipoTesouro.IPCA);

        Page<TituloTesouroListagemResponseDTO> resultado = tituloTesouroListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("IPCA-1");
    }

    @Test
    @DisplayName("listar - deve excluir títulos com vencimento após vencimentoAte")
    void listar_comFiltroVencimentoAte_deveExcluirVencimentosPosteriores() {
        TituloTesouro curto = criarTitulo("CURTO", TipoTesouro.SELIC, BigDecimal.valueOf(0.08), BigDecimal.valueOf(150), LocalDate.of(2027, 1, 1));
        TituloTesouro longo = criarTitulo("LONGO", TipoTesouro.SELIC, BigDecimal.valueOf(0.08), BigDecimal.valueOf(150), LocalDate.of(2035, 1, 1));

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(curto, longo));

        TituloTesouroListagemFiltroDTO filtro = new TituloTesouroListagemFiltroDTO();
        filtro.setVencimentoAte(LocalDate.of(2030, 1, 1));

        Page<TituloTesouroListagemResponseDTO> resultado = tituloTesouroListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("CURTO");
    }

    @Test
    @DisplayName("listar - deve excluir títulos abaixo da taxa mínima")
    void listar_comFiltroTaxaMinima_deveExcluirAbaixoDoMinimo() {
        TituloTesouro taxaBaixa = criarTitulo("BAIXA", TipoTesouro.PREFIXADO, BigDecimal.valueOf(4.0), BigDecimal.valueOf(150), LocalDate.of(2031, 1, 1));
        TituloTesouro taxaAlta = criarTitulo("ALTA", TipoTesouro.PREFIXADO, BigDecimal.valueOf(12.0), BigDecimal.valueOf(150), LocalDate.of(2031, 1, 1));

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(taxaBaixa, taxaAlta));

        TituloTesouroListagemFiltroDTO filtro = new TituloTesouroListagemFiltroDTO();
        filtro.setTaxaMinima(BigDecimal.valueOf(6.0));

        Page<TituloTesouroListagemResponseDTO> resultado = tituloTesouroListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("ALTA");
    }

    @Test
    @DisplayName("listar - ordenarPor TAXA deve ordenar decrescente (maior taxa primeiro)")
    void listar_ordenarPorTaxa_deveOrdenarDecrescente() {
        TituloTesouro taxaBaixa = criarTitulo("BAIXA", TipoTesouro.PREFIXADO, BigDecimal.valueOf(4.0), BigDecimal.valueOf(150), LocalDate.of(2031, 1, 1));
        TituloTesouro taxaAlta = criarTitulo("ALTA", TipoTesouro.PREFIXADO, BigDecimal.valueOf(12.0), BigDecimal.valueOf(150), LocalDate.of(2031, 1, 1));

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(taxaBaixa, taxaAlta));

        TituloTesouroListagemFiltroDTO filtro = new TituloTesouroListagemFiltroDTO();
        filtro.setOrdenarPor(OrdenarPorTesouro.TAXA);

        Page<TituloTesouroListagemResponseDTO> resultado = tituloTesouroListagemService.listar(filtro);

        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("ALTA");
        assertThat(resultado.getContent().get(1).getCodigo()).isEqualTo("BAIXA");
    }

    @Test
    @DisplayName("listar - ordenarPor PRECO_MINIMO deve ordenar crescente (mais barato primeiro)")
    void listar_ordenarPorPrecoMinimo_deveOrdenarCrescente() {
        TituloTesouro caro = criarTitulo("CARO", TipoTesouro.SELIC, BigDecimal.valueOf(0.08), BigDecimal.valueOf(500), LocalDate.of(2031, 1, 1));
        TituloTesouro barato = criarTitulo("BARATO", TipoTesouro.SELIC, BigDecimal.valueOf(0.08), BigDecimal.valueOf(50), LocalDate.of(2031, 1, 1));

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(caro, barato));

        TituloTesouroListagemFiltroDTO filtro = new TituloTesouroListagemFiltroDTO();
        filtro.setOrdenarPor(OrdenarPorTesouro.PRECO_MINIMO);

        Page<TituloTesouroListagemResponseDTO> resultado = tituloTesouroListagemService.listar(filtro);

        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("BARATO");
        assertThat(resultado.getContent().get(1).getCodigo()).isEqualTo("CARO");
    }

    @Test
    @DisplayName("listar - deve paginar corretamente")
    void listar_devePaginarCorretamente() {
        List<TituloTesouro> titulos = List.of(
                criarTitulo("T1", TipoTesouro.SELIC, BigDecimal.valueOf(0.08), BigDecimal.valueOf(150), LocalDate.of(2028, 1, 1)),
                criarTitulo("T2", TipoTesouro.SELIC, BigDecimal.valueOf(0.08), BigDecimal.valueOf(150), LocalDate.of(2029, 1, 1)),
                criarTitulo("T3", TipoTesouro.SELIC, BigDecimal.valueOf(0.08), BigDecimal.valueOf(150), LocalDate.of(2030, 1, 1))
        );

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(titulos);

        TituloTesouroListagemFiltroDTO filtro = new TituloTesouroListagemFiltroDTO();
        filtro.setPagina(2);
        filtro.setTamanho(2);

        Page<TituloTesouroListagemResponseDTO> resultado = tituloTesouroListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1); // só sobra T3 na página 2
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("T3");
        assertThat(resultado.getTotalElements()).isEqualTo(3);
    }
}