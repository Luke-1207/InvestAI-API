package com.investai.api.module.relatorio.builder;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.dto.ValorDescritoDTO;
import com.investai.api.module.relatorio.PdfTesteUtil;
import com.investai.api.module.relatorio.component.PdfDisclaimerComponent;
import com.investai.api.module.relatorio.component.PdfHeaderComponent;
import com.investai.api.module.relatorio.component.PdfSecaoComponent;
import com.investai.api.module.relatorio.component.PdfTableComponent;
import com.investai.api.module.relatorio.dto.DadosRelatorioListagem;
import com.investai.api.module.relatorio.dto.ItemListagemRelatorio;
import com.investai.api.module.relatorio.dto.ModuloRelatorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioListagemBuilderTest {

    private final RelatorioListagemBuilder relatorioListagemBuilder = new RelatorioListagemBuilder(
            new PdfHeaderComponent(), new PdfTableComponent(), new PdfSecaoComponent(), new PdfDisclaimerComponent());

    private PerfilResponseDTO perfil() {
        return PerfilResponseDTO.builder()
                .perfilPreenchido(true)
                .perfilRisco(ValorDescritoDTO.builder().valor("MODERADO").build())
                .objetivoFinanceiro(ValorDescritoDTO.builder().valor("CRESCIMENTO_PATRIMONIO").build())
                .horizonteInvestimento(ValorDescritoDTO.builder().valor("LONGO_PRAZO").build())
                .valorDisponivel(new BigDecimal("15000"))
                .build();
    }

    private ItemListagemRelatorio item(String codigo, int score) {
        return ItemListagemRelatorio.builder()
                .codigo(codigo)
                .nome("Empresa " + codigo)
                .tipo("Ação")
                .dadoPrincipal("DY 8,40%")
                .score(score)
                .compatibilidade(Compatibilidade.MEDIA)
                .justificativa("Setor está entre os que você prefere")
                .build();
    }

    private DadosRelatorioListagem.DadosRelatorioListagemBuilder dados() {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("Tipo", "Ações");
        filtros.put("Modo", "Inteligente");

        return DadosRelatorioListagem.builder()
                .nomeUsuario("Lucas Silva")
                .geradoEm(LocalDateTime.of(2026, 10, 6, 14, 30))
                .perfil(perfil())
                .modulo(ModuloRelatorio.VARIAVEL)
                .filtros(filtros)
                .itens(List.of(item("TAEE11", 62), item("ITUB4", 55)))
                .totalNaListagem(2)
                .ativosNaoEncontrados(List.of());
    }

    private String textoDe(byte[] pdf) {
        return PdfTesteUtil.extrairTexto(pdf).replaceAll("\\s+", " ");
    }

    @Test
    @DisplayName("construir - deve incluir perfil, filtros, tabela de ativos e aviso")
    void construir_deveIncluirTodasAsSecoes() {
        byte[] pdf = relatorioListagemBuilder.construir(dados().build());

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(textoDe(pdf))
                .contains("Listagem ranqueada: Renda Variável", "Gerado para Lucas Silva")
                .contains("Moderado", "Crescimento de patrimônio", "Longo prazo (mais de 5 anos)", "R$ 15.000,00")
                .contains("Filtros utilizados", "Tipo", "Ações", "Modo", "Inteligente")
                .contains("Código", "Dado principal", "Score", "Compatibilidade", "Justificativa")
                .contains("TAEE11", "Empresa TAEE11", "DY 8,40%", "62", "Média", "Setor está entre os que você prefere")
                .contains("ITUB4", "55")
                .contains(PdfDisclaimerComponent.TITULO)
                .doesNotContain(RelatorioListagemBuilder.SEM_FILTROS)
                .doesNotContain("Mostrando os")
                .doesNotContain("Não encontrados");
    }

    @Test
    @DisplayName("construir - título deve acompanhar o módulo")
    void construir_tituloDeveAcompanharModulo() {
        assertThat(textoDe(relatorioListagemBuilder.construir(dados().modulo(ModuloRelatorio.FIXA).build())))
                .contains("Listagem ranqueada: Renda Fixa");
        assertThat(textoDe(relatorioListagemBuilder.construir(dados().modulo(ModuloRelatorio.AMBOS).build())))
                .contains("Listagem ranqueada: Renda Variável e Renda Fixa");
    }

    @Test
    @DisplayName("construir - sem filtros deve mostrar a nota de nenhum filtro aplicado")
    void construir_semFiltros_deveMostrarNota() {
        assertThat(textoDe(relatorioListagemBuilder.construir(dados().filtros(Map.of()).build())))
                .contains(RelatorioListagemBuilder.SEM_FILTROS);
        assertThat(textoDe(relatorioListagemBuilder.construir(dados().filtros(null).build())))
                .contains(RelatorioListagemBuilder.SEM_FILTROS);
    }

    @Test
    @DisplayName("construir - listagem maior que o limite deve informar quantos ativos ficaram de fora")
    void construir_listagemTruncada_deveInformarTotal() {
        byte[] pdf = relatorioListagemBuilder.construir(dados().totalNaListagem(80).build());

        assertThat(textoDe(pdf)).contains("Mostrando os 2 ativos mais bem pontuados de 80 na listagem.");
    }

    @Test
    @DisplayName("construir - deve listar os códigos pedidos que não estavam na listagem")
    void construir_ativosNaoEncontrados_deveListarCodigos() {
        byte[] pdf = relatorioListagemBuilder.construir(dados().ativosNaoEncontrados(List.of("XPTO3", "ABCD4")).build());

        assertThat(textoDe(pdf)).contains("Não encontrados na listagem ranqueada: XPTO3, ABCD4.");
    }

    @Test
    @DisplayName("construir - justificativa longa deve ser resumida com reticências")
    void construir_justificativaLonga_deveSerResumida() {
        String longa = "Palavra ".repeat(40).trim();
        ItemListagemRelatorio item = ItemListagemRelatorio.builder()
                .codigo("TAEE11").nome("Transmissora").tipo("Ação").dadoPrincipal("DY 8,40%")
                .score(62).compatibilidade(Compatibilidade.ALTA).justificativa(longa + " FIM_DA_JUSTIFICATIVA")
                .build();

        String texto = textoDe(relatorioListagemBuilder.construir(dados().itens(List.of(item)).build()));

        assertThat(texto).contains("Alta", "Palavra...").doesNotContain("FIM_DA_JUSTIFICATIVA");
    }

    @Test
    @DisplayName("construir - justificativa longa sem espaços deve ser cortada no limite de caracteres")
    void construir_justificativaSemEspacos_deveCortarNoLimite() {
        ItemListagemRelatorio item = ItemListagemRelatorio.builder()
                .codigo("TAEE11").nome("Transmissora").tipo("Ação")
                .justificativa("x".repeat(300))
                .build();

        String texto = PdfTesteUtil.extrairTexto(relatorioListagemBuilder.construir(dados().itens(List.of(item)).build()))
                .replaceAll("\\s+", "");

        assertThat(texto)
                .contains("x".repeat(RelatorioListagemBuilder.TAMANHO_MAXIMO_JUSTIFICATIVA - 3) + "...")
                .doesNotContain("x".repeat(RelatorioListagemBuilder.TAMANHO_MAXIMO_JUSTIFICATIVA - 2));
    }

    @Test
    @DisplayName("construir - item sem score, compatibilidade ou dado principal deve mostrar traço")
    void construir_itemIncompleto_deveMostrarTraco() {
        ItemListagemRelatorio item = ItemListagemRelatorio.builder()
                .codigo("-").nome("CDB - Banco Alfa").tipo("CDB").build();

        byte[] pdf = relatorioListagemBuilder.construir(dados().itens(List.of(item)).build());

        assertThat(textoDe(pdf)).contains("CDB - Banco Alfa CDB - - - -");
    }

    @Test
    @DisplayName("construir - 50 ativos devem caber no relatório, com o cabeçalho da tabela repetido nas páginas")
    void construir_cinquentaAtivos_deveRepetirCabecalho() {
        List<ItemListagemRelatorio> itens = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            itens.add(item("ATV" + i, 100 - i));
        }

        byte[] pdf = relatorioListagemBuilder.construir(dados().itens(itens).totalNaListagem(50).build());

        assertThat(PdfTesteUtil.contarPaginas(pdf)).isGreaterThan(1);
        assertThat(PdfTesteUtil.extrairTextoDaPagina(pdf, 2)).contains("Dado principal");
        assertThat(textoDe(pdf)).contains("ATV1 ", "ATV50 ");
    }
}
