package com.investai.api.module.rendafixa.service;

import com.investai.api.module.rendafixa.dto.OrdenarPorTituloPrivado;
import com.investai.api.module.rendafixa.dto.TituloPrivadoListagemFiltroDTO;
import com.investai.api.module.rendafixa.dto.TituloPrivadoListagemResponseDTO;
import com.investai.api.module.rendafixa.entity.*;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
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
class TituloPrivadoListagemServiceTest {

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @InjectMocks
    private TituloPrivadoListagemService tituloPrivadoListagemService;

    private TituloPrivado criarTitulo(TipoTituloPrivado tipo, Indexador indexador, TipoLiquidez liquidez,
                                      boolean isentoIr, BigDecimal taxa, BigDecimal investimentoMinimo, LocalDate vencimento) {
        return TituloPrivado.builder()
                .id(UUID.randomUUID())
                .tipo(tipo)
                .emissor("Emissor " + tipo)
                .indexador(indexador)
                .taxaPercentual(taxa)
                .vencimento(vencimento)
                .investimentoMinimo(investimentoMinimo)
                .liquidez(liquidez)
                .garantidoFgc(true)
                .isentoIr(isentoIr)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("listar - deve filtrar por tipo (múltiplo)")
    void listar_deveFiltrarPorTipoMultiplo() {
        TituloPrivado cdb = criarTitulo(TipoTituloPrivado.CDB, Indexador.CDI, TipoLiquidez.DIARIA, false, BigDecimal.valueOf(100), BigDecimal.valueOf(500), LocalDate.now().plusYears(1));
        TituloPrivado lci = criarTitulo(TipoTituloPrivado.LCI, Indexador.CDI, TipoLiquidez.NO_VENCIMENTO, true, BigDecimal.valueOf(95), BigDecimal.valueOf(1000), LocalDate.now().plusYears(2));
        TituloPrivado lca = criarTitulo(TipoTituloPrivado.LCA, Indexador.CDI, TipoLiquidez.NO_VENCIMENTO, true, BigDecimal.valueOf(96), BigDecimal.valueOf(1000), LocalDate.now().plusYears(2));

        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of(cdb, lci, lca));

        TituloPrivadoListagemFiltroDTO filtro = new TituloPrivadoListagemFiltroDTO();
        filtro.setTipo(List.of(TipoTituloPrivado.LCI, TipoTituloPrivado.LCA));

        Page<TituloPrivadoListagemResponseDTO> resultado = tituloPrivadoListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent()).extracting(TituloPrivadoListagemResponseDTO::getTipo)
                .containsExactlyInAnyOrder(TipoTituloPrivado.LCI, TipoTituloPrivado.LCA);
    }

    @Test
    @DisplayName("listar - deve filtrar por isentoIR")
    void listar_deveFiltrarPorIsentoIR() {
        TituloPrivado cdb = criarTitulo(TipoTituloPrivado.CDB, Indexador.CDI, TipoLiquidez.DIARIA, false, BigDecimal.valueOf(100), BigDecimal.valueOf(500), LocalDate.now().plusYears(1));
        TituloPrivado lci = criarTitulo(TipoTituloPrivado.LCI, Indexador.CDI, TipoLiquidez.NO_VENCIMENTO, true, BigDecimal.valueOf(95), BigDecimal.valueOf(1000), LocalDate.now().plusYears(2));

        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of(cdb, lci));

        TituloPrivadoListagemFiltroDTO filtro = new TituloPrivadoListagemFiltroDTO();
        filtro.setIsentoIR(true);

        Page<TituloPrivadoListagemResponseDTO> resultado = tituloPrivadoListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getTipo()).isEqualTo(TipoTituloPrivado.LCI);
    }

    @Test
    @DisplayName("listar - deve filtrar por investimentoMaximo")
    void listar_deveFiltrarPorInvestimentoMaximo() {
        TituloPrivado barato = criarTitulo(TipoTituloPrivado.CDB, Indexador.CDI, TipoLiquidez.DIARIA, false, BigDecimal.valueOf(100), BigDecimal.valueOf(500), LocalDate.now().plusYears(1));
        TituloPrivado caro = criarTitulo(TipoTituloPrivado.CDB, Indexador.CDI, TipoLiquidez.DIARIA, false, BigDecimal.valueOf(105), BigDecimal.valueOf(50000), LocalDate.now().plusYears(1));

        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of(barato, caro));

        TituloPrivadoListagemFiltroDTO filtro = new TituloPrivadoListagemFiltroDTO();
        filtro.setInvestimentoMaximo(BigDecimal.valueOf(1000));

        Page<TituloPrivadoListagemResponseDTO> resultado = tituloPrivadoListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getInvestimentoMinimo()).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("listar - ordenarPor TAXA (padrão) deve ordenar decrescente")
    void listar_ordenarPorTaxaPadrao_deveOrdenarDecrescente() {
        TituloPrivado taxaBaixa = criarTitulo(TipoTituloPrivado.CDB, Indexador.CDI, TipoLiquidez.DIARIA, false, BigDecimal.valueOf(95), BigDecimal.valueOf(500), LocalDate.now().plusYears(1));
        TituloPrivado taxaAlta = criarTitulo(TipoTituloPrivado.CDB, Indexador.CDI, TipoLiquidez.DIARIA, false, BigDecimal.valueOf(115), BigDecimal.valueOf(500), LocalDate.now().plusYears(1));

        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of(taxaBaixa, taxaAlta));

        Page<TituloPrivadoListagemResponseDTO> resultado = tituloPrivadoListagemService.listar(new TituloPrivadoListagemFiltroDTO());

        assertThat(resultado.getContent().get(0).getTaxaPercentual()).isEqualByComparingTo("115");
        assertThat(resultado.getContent().get(1).getTaxaPercentual()).isEqualByComparingTo("95");
    }

    @Test
    @DisplayName("listar - deve paginar corretamente")
    void listar_devePaginarCorretamente() {
        List<TituloPrivado> titulos = List.of(
                criarTitulo(TipoTituloPrivado.CDB, Indexador.CDI, TipoLiquidez.DIARIA, false, BigDecimal.valueOf(100), BigDecimal.valueOf(500), LocalDate.now().plusYears(1)),
                criarTitulo(TipoTituloPrivado.CDB, Indexador.CDI, TipoLiquidez.DIARIA, false, BigDecimal.valueOf(101), BigDecimal.valueOf(500), LocalDate.now().plusYears(1)),
                criarTitulo(TipoTituloPrivado.CDB, Indexador.CDI, TipoLiquidez.DIARIA, false, BigDecimal.valueOf(102), BigDecimal.valueOf(500), LocalDate.now().plusYears(1))
        );

        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(titulos);

        TituloPrivadoListagemFiltroDTO filtro = new TituloPrivadoListagemFiltroDTO();
        filtro.setOrdenarPor(OrdenarPorTituloPrivado.VENCIMENTO);
        filtro.setPagina(1);
        filtro.setTamanho(2);

        Page<TituloPrivadoListagemResponseDTO> resultado = tituloPrivadoListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getTotalElements()).isEqualTo(3);
    }
}