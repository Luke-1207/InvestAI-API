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
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcaoListagemServiceTest {

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private CotacaoService cotacaoService;

    @InjectMocks
    private AcaoListagemService acaoListagemService;

    @Test
    @DisplayName("listar - deve buscar todos os ativos quando nenhum filtro de catálogo informado")
    void listar_deveBuscarTodosQuandoSemFiltros() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(criarAcao("TAEE3", TipoAtivo.ACAO, "Energia")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8, 1.25));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        acaoListagemService.listar(filtro);

        verify(acaoRepository).findByAtivoTrue();
        verify(acaoRepository, never()).findByAtivoTrueAndTipoIn(any());
        verify(acaoRepository, never()).findByAtivoTrueAndSetorIgnoreCase(any());
    }

    @Test
    @DisplayName("listar - deve buscar por tipo quando apenas tipo informado")
    void listar_deveBuscarPorTipoQuandoApenasTipoInformado() {
        when(acaoRepository.findByAtivoTrueAndTipoIn(List.of(TipoAtivo.ACAO)))
                .thenReturn(List.of(criarAcao("TAEE3", TipoAtivo.ACAO, "Energia")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8, 1.25));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setTipo(List.of(TipoAtivo.ACAO));
        acaoListagemService.listar(filtro);

        verify(acaoRepository).findByAtivoTrueAndTipoIn(List.of(TipoAtivo.ACAO));
        verify(acaoRepository, never()).findByAtivoTrue();
    }

    @Test
    @DisplayName("listar - deve buscar por setor quando apenas setor informado")
    void listar_deveBuscarPorSetorQuandoApenasSetorInformado() {
        when(acaoRepository.findByAtivoTrueAndSetorIgnoreCase("Energia"))
                .thenReturn(List.of(criarAcao("TAEE3", TipoAtivo.ACAO, "Energia")));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8, 1.25));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setSetor("Energia");
        acaoListagemService.listar(filtro);

        verify(acaoRepository).findByAtivoTrueAndSetorIgnoreCase("Energia");
    }

    @Test
    @DisplayName("listar - deve buscar por tipo e setor quando ambos informados")
    void listar_deveBuscarPorTipoESetorQuandoAmbosInformados() {
        when(acaoRepository.findByAtivoTrueAndTipoInAndSetorIgnoreCase(List.of(TipoAtivo.FII), "Logística"))
                .thenReturn(List.of(criarAcao("HGLG11", TipoAtivo.FII, "Logística")));
        when(cotacaoService.obterCotacao("HGLG11")).thenReturn(criarCotacao("HGLG11", 148.15, 8.9, 0.37));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setTipo(List.of(TipoAtivo.FII));
        filtro.setSetor("Logística");
        acaoListagemService.listar(filtro);

        verify(acaoRepository).findByAtivoTrueAndTipoInAndSetorIgnoreCase(List.of(TipoAtivo.FII), "Logística");
    }

    @Test
    @DisplayName("listar - deve marcar cotacaoDisponivel=false quando serviço de cotação não encontra o ticker")
    void listar_deveMarcarCotacaoIndisponivelQuandoNaoEncontrada() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(criarAcao("NOVA3", TipoAtivo.ACAO, "Setor")));
        when(cotacaoService.obterCotacao("NOVA3"))
                .thenThrow(new ResourceNotFoundException("Nenhuma cotação encontrada"));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isCotacaoDisponivel()).isFalse();
        assertThat(resultado.getContent().get(0).getPreco()).isNull();
    }

    @Test
    @DisplayName("listar - deve filtrar por dyMinimo excluindo ativos abaixo do valor")
    void listar_deveFiltrarPorDyMinimo() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(
                criarAcao("TAEE3", TipoAtivo.ACAO, "Energia"),
                criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo")
        ));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8, 1.25));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao("PETR4", 38.90, 3.2, -0.42));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setDyMinimo(BigDecimal.valueOf(6));

        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("TAEE3");
    }

    @Test
    @DisplayName("listar - deve filtrar por precoMaximo excluindo ativos acima do valor")
    void listar_deveFiltrarPorPrecoMaximo() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(
                criarAcao("TAEE3", TipoAtivo.ACAO, "Energia"),
                criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo")
        ));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8, 1.25));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao("PETR4", 90.00, 3.2, -0.42));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setPrecoMaximo(BigDecimal.valueOf(40));

        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("TAEE3");
    }

    @Test
    @DisplayName("listar - deve excluir ativo com cotação indisponível quando filtro de mercado aplicado")
    void listar_deveExcluirAtivoSemCotacaoQuandoFiltroDeMercadoAplicado() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(criarAcao("NOVA3", TipoAtivo.ACAO, "Setor")));
        when(cotacaoService.obterCotacao("NOVA3"))
                .thenThrow(new ResourceNotFoundException("Nenhuma cotação encontrada"));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setDyMinimo(BigDecimal.valueOf(5));

        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    @DisplayName("listar - deve ordenar por nome ascendente por padrão")
    void listar_deveOrdenarPorNomeAscendentePorPadrao() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(
                criarAcaoComNome("VALE3", "Vale"),
                criarAcaoComNome("TAEE3", "Ambev")
        ));
        when(cotacaoService.obterCotacao(anyString())).thenReturn(criarCotacao("X", 10.0, 1.0, 0.1));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent().get(0).getNome()).isEqualTo("Ambev");
        assertThat(resultado.getContent().get(1).getNome()).isEqualTo("Vale");
    }

    @Test
    @DisplayName("listar - deve ordenar por DY decrescente quando solicitado")
    void listar_deveOrdenarPorDyDescendente() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(
                criarAcao("TAEE3", TipoAtivo.ACAO, "Energia"),
                criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo")
        ));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8, 1.25));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao("PETR4", 38.90, 14.2, -0.42));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setOrdenarPor(OrdenarPorAcao.DY);
        filtro.setOrdem(OrdemDTO.DESC);

        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("PETR4");
        assertThat(resultado.getContent().get(1).getCodigo()).isEqualTo("TAEE3");
    }

    @Test
    @DisplayName("listar - deve ordenar por preço ascendente quando solicitado")
    void listar_deveOrdenarPorPrecoAscendente() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(
                criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo"),
                criarAcao("TAEE3", TipoAtivo.ACAO, "Energia")
        ));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao("PETR4", 90.0, 3.2, -0.42));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8, 1.25));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setOrdenarPor(OrdenarPorAcao.PRECO);
        filtro.setOrdem(OrdemDTO.ASC);

        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("TAEE3");
        assertThat(resultado.getContent().get(1).getCodigo()).isEqualTo("PETR4");
    }

    @Test
    @DisplayName("listar - deve ordenar por variação do dia quando solicitado")
    void listar_deveOrdenarPorVariacaoDia() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(
                criarAcao("TAEE3", TipoAtivo.ACAO, "Energia"),
                criarAcao("PETR4", TipoAtivo.ACAO, "Petróleo")
        ));
        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(criarCotacao("TAEE3", 38.42, 6.8, 1.25));
        when(cotacaoService.obterCotacao("PETR4")).thenReturn(criarCotacao("PETR4", 38.90, 3.2, -0.42));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setOrdenarPor(OrdenarPorAcao.VARIACAO_DIA);
        filtro.setOrdem(OrdemDTO.DESC);

        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("TAEE3");
    }

    @Test
    @DisplayName("listar - deve paginar corretamente respeitando tamanho informado")
    void listar_devePaginarCorretamente() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(
                criarAcaoComNome("A3", "Ativo A"),
                criarAcaoComNome("B3", "Ativo B"),
                criarAcaoComNome("C3", "Ativo C")
        ));
        when(cotacaoService.obterCotacao(anyString())).thenReturn(criarCotacao("X", 10.0, 1.0, 0.1));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setPagina(1);
        filtro.setTamanho(2);

        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("listar - deve retornar segunda página com o restante dos itens")
    void listar_deveRetornarSegundaPaginaComRestante() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(
                criarAcaoComNome("A3", "Ativo A"),
                criarAcaoComNome("B3", "Ativo B"),
                criarAcaoComNome("C3", "Ativo C")
        ));
        when(cotacaoService.obterCotacao(anyString())).thenReturn(criarCotacao("X", 10.0, 1.0, 0.1));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setPagina(2);
        filtro.setTamanho(2);

        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNome()).isEqualTo("Ativo C");
    }

    @Test
    @DisplayName("listar - deve retornar página vazia quando página solicitada está além do total")
    void listar_deveRetornarPaginaVaziaQuandoAlemDoTotal() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(criarAcaoComNome("A3", "Ativo A")));
        when(cotacaoService.obterCotacao(anyString())).thenReturn(criarCotacao("X", 10.0, 1.0, 0.1));

        AcaoListagemFiltroDTO filtro = new AcaoListagemFiltroDTO();
        filtro.setPagina(5);
        filtro.setTamanho(10);

        Page<AcaoListagemResponseDTO> resultado = acaoListagemService.listar(filtro);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    private Acao criarAcao(String codigo, TipoAtivo tipo, String setor) {
        return Acao.builder()
                .id(UUID.randomUUID())
                .codigo(codigo)
                .nome(codigo + " Empresa Teste")
                .tipo(tipo)
                .setor(setor)
                .ativo(true)
                .build();
    }

    private Acao criarAcaoComNome(String codigo, String nome) {
        return Acao.builder()
                .id(UUID.randomUUID())
                .codigo(codigo)
                .nome(nome)
                .tipo(TipoAtivo.ACAO)
                .setor("Setor Teste")
                .ativo(true)
                .build();
    }

    private CotacaoResponseDTO criarCotacao(String codigo, double preco, double dy, double variacao) {
        return CotacaoResponseDTO.builder()
                .codigo(codigo)
                .preco(BigDecimal.valueOf(preco))
                .dividendYield(BigDecimal.valueOf(dy))
                .variacaoPercentual(BigDecimal.valueOf(variacao))
                .fonte("MOCK")
                .build();
    }
}