package com.investai.api.module.relatorio.builder;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.dto.ValorDescritoDTO;
import com.investai.api.module.relatorio.PdfTesteUtil;
import com.investai.api.module.relatorio.component.PdfDisclaimerComponent;
import com.investai.api.module.relatorio.component.PdfHeaderComponent;
import com.investai.api.module.relatorio.component.PdfSecaoComponent;
import com.investai.api.module.relatorio.component.PdfTableComponent;
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoFixo;
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoVariavel;
import com.investai.api.module.relatorio.dto.RentabilidadePrazoDTO;
import com.investai.api.module.relatorio.dto.TituloRendaFixaRelatorio;
import com.investai.api.module.rendafixa.entity.Indexador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioAtivoBuilderTest {

    private static final LocalDateTime GERADO_EM = LocalDateTime.of(2026, 10, 6, 14, 30);

    private final RelatorioAtivoBuilder relatorioAtivoBuilder = new RelatorioAtivoBuilder(
            new PdfHeaderComponent(), new PdfTableComponent(), new PdfSecaoComponent(), new PdfDisclaimerComponent());

    private PerfilResponseDTO perfil(boolean preenchido) {
        return PerfilResponseDTO.builder()
                .perfilPreenchido(preenchido)
                .perfilRisco(ValorDescritoDTO.builder().valor("MODERADO").build())
                .objetivoFinanceiro(ValorDescritoDTO.builder().valor("CRESCIMENTO_PATRIMONIO").build())
                .horizonteInvestimento(ValorDescritoDTO.builder().valor("LONGO_PRAZO").build())
                .valorDisponivel(new BigDecimal("15000"))
                .build();
    }

    private AcaoDetalheResponseDTO.AcaoDetalheResponseDTOBuilder ativoComCotacao() {
        return AcaoDetalheResponseDTO.builder()
                .codigo("TAEE11")
                .nome("Transmissora Aliança")
                .tipo(TipoAtivo.ACAO)
                .setor("Energia Elétrica")
                .cotacaoDisponivel(true)
                .preco(new BigDecimal("38.42"))
                .variacaoPercentual(new BigDecimal("1.23"))
                .dividendYield(new BigDecimal("8.4"))
                .precoValorPatrimonial(new BigDecimal("1.35"))
                .minimo52Semanas(new BigDecimal("31.2"))
                .maximo52Semanas(new BigDecimal("41.9"))
                .cotacaoAtualizadaEm(LocalDateTime.of(2026, 10, 6, 10, 0))
                .fonteCotacao("HG Brasil");
    }

    private TituloRendaFixaRelatorio.TituloRendaFixaRelatorioBuilder tituloCdb() {
        return TituloRendaFixaRelatorio.builder()
                .identificador("CDB-Banco Alfa")
                .nome("CDB Banco Alfa")
                .categoria("CDB")
                .indexador(Indexador.CDI)
                .taxa(new BigDecimal("110"))
                .taxaSomadaAoIndexador(false)
                .vencimento(LocalDate.of(2029, 3, 24))
                .investimentoMinimo(new BigDecimal("1000"))
                .liquidez("No vencimento")
                .garantia("Garantido pelo FGC")
                .isentoIr(false)
                .rentabilidades(List.of(
                        new RentabilidadePrazoDTO("180 dias", new BigDecimal("22.5"), new BigDecimal("110"), new BigDecimal("85.25")),
                        new RentabilidadePrazoDTO("No vencimento (900 dias)", new BigDecimal("15.0"), new BigDecimal("110"), new BigDecimal("93.50"))));
    }

    private String textoDe(byte[] pdf) {
        return PdfTesteUtil.extrairTexto(pdf).replaceAll("\\s+", " ");
    }

    @Test
    @DisplayName("construir (variável) - deve incluir perfil, indicadores, compatibilidade, resumo e aviso")
    void construirVariavel_deveIncluirTodasAsSecoes() {
        byte[] pdf = relatorioAtivoBuilder.construir(DadosRelatorioAtivoVariavel.builder()
                .nomeUsuario("Lucas Silva")
                .geradoEm(GERADO_EM)
                .perfil(perfil(true))
                .ativo(ativoComCotacao().build())
                .compatibilidade(SugestaoAtivoItemDTO.builder()
                        .score(62)
                        .compatibilidade(Compatibilidade.MEDIA)
                        .justificativa("Setor está entre os que você prefere")
                        .build())
                .resumoIa("Resumo gerado pela IA para o ativo.")
                .build());

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(textoDe(pdf))
                .contains("Análise de ativo: TAEE11", "Gerado para Lucas Silva")
                .contains("Transmissora Aliança", "Ação · Energia Elétrica")
                .contains("Moderado", "Crescimento de patrimônio", "Longo prazo (mais de 5 anos)", "R$ 15.000,00")
                .contains("R$ 38,42", "+1,23%", "8,40%", "1,35", "R$ 31,20", "R$ 41,90")
                .contains("Cotação atualizada em 06/10/2026 às 10:00. Fonte: HG Brasil.")
                .contains("Score 62 de 100", "Compatibilidade Média", "Setor está entre os que você prefere")
                .contains("Resumo gerado pela IA para o ativo.")
                .contains(PdfDisclaimerComponent.TITULO)
                .doesNotContain(RelatorioAtivoBuilder.RESUMO_INDISPONIVEL);
    }

    @Test
    @DisplayName("construir (variável) - sem resumo de IA deve mostrar a nota de indisponibilidade")
    void construirVariavel_semResumo_deveMostrarNotaDeIndisponibilidade() {
        byte[] pdf = relatorioAtivoBuilder.construir(DadosRelatorioAtivoVariavel.builder()
                .nomeUsuario("Lucas Silva")
                .geradoEm(GERADO_EM)
                .perfil(perfil(true))
                .ativo(ativoComCotacao().build())
                .resumoIa("  ")
                .build());

        assertThat(textoDe(pdf))
                .contains(RelatorioAtivoBuilder.RESUMO_INDISPONIVEL)
                .contains(RelatorioAtivoBuilder.COMPATIBILIDADE_INDISPONIVEL);
    }

    @Test
    @DisplayName("construir (variável) - perfil não preenchido deve orientar a responder o quiz")
    void construirVariavel_perfilNaoPreenchido_deveOrientarAResponderOQuiz() {
        byte[] pdf = relatorioAtivoBuilder.construir(DadosRelatorioAtivoVariavel.builder()
                .nomeUsuario("Lucas Silva")
                .geradoEm(GERADO_EM)
                .perfil(perfil(false))
                .ativo(ativoComCotacao().build())
                .build());

        assertThat(textoDe(pdf))
                .contains(RelatorioAtivoBuilder.PERFIL_INCOMPLETO)
                .doesNotContain("Crescimento de patrimônio")
                .doesNotContain(RelatorioAtivoBuilder.COMPATIBILIDADE_INDISPONIVEL);
    }

    @Test
    @DisplayName("construir (variável) - ativo sem cotação deve gerar o PDF com a nota de cotação indisponível")
    void construirVariavel_semCotacao_deveGerarComNota() {
        byte[] pdf = relatorioAtivoBuilder.construir(DadosRelatorioAtivoVariavel.builder()
                .nomeUsuario("Lucas Silva")
                .geradoEm(GERADO_EM)
                .perfil(perfil(true))
                .ativo(AcaoDetalheResponseDTO.builder()
                        .codigo("XPTO3").nome("Empresa XPTO").tipo(TipoAtivo.FII).setor("Logística")
                        .cotacaoDisponivel(false)
                        .build())
                .build());

        assertThat(textoDe(pdf))
                .contains("Fundo imobiliário · Logística")
                .contains(RelatorioAtivoBuilder.COTACAO_INDISPONIVEL)
                .doesNotContain("Cotação atualizada em");
    }

    @Test
    @DisplayName("construir (fixa) - deve incluir dados do título e a tabela de rentabilidade por prazo")
    void construirFixa_deveIncluirDadosERentabilidade() {
        byte[] pdf = relatorioAtivoBuilder.construir(DadosRelatorioAtivoFixo.builder()
                .nomeUsuario("Lucas Silva")
                .geradoEm(GERADO_EM)
                .perfil(perfil(true))
                .titulo(tituloCdb().build())
                .build());

        assertThat(textoDe(pdf))
                .contains("Análise de título de renda fixa", "CDB Banco Alfa")
                .contains("CDI (pós-fixado)", "110,00% do CDI", "24/03/2029", "R$ 1.000,00")
                .contains("No vencimento", "Garantido pelo FGC", "Tributado (tabela regressiva)")
                .contains("180 dias", "22,50%", "85,25% do CDI")
                .contains("No vencimento (900 dias)", "15,00%", "93,50% do CDI")
                .contains(RelatorioAtivoBuilder.NOTA_RENTABILIDADE)
                .contains(PdfDisclaimerComponent.TITULO);
    }

    @Test
    @DisplayName("construir (fixa) - taxa somada ao índice deve mostrar a parcela do rendimento em vez da taxa líquida")
    void construirFixa_taxaSomadaAoIndice_deveMostrarParcelaDoRendimento() {
        byte[] pdf = relatorioAtivoBuilder.construir(DadosRelatorioAtivoFixo.builder()
                .nomeUsuario("Lucas Silva")
                .geradoEm(GERADO_EM)
                .perfil(perfil(true))
                .titulo(tituloCdb()
                        .nome("Tesouro IPCA+ 2035")
                        .categoria("Tesouro Direto")
                        .indexador(Indexador.IPCA)
                        .taxa(new BigDecimal("6.2"))
                        .taxaSomadaAoIndexador(true)
                        .rentabilidades(List.of(new RentabilidadePrazoDTO(
                                "180 dias", new BigDecimal("22.5"), new BigDecimal("6.2"), null)))
                        .build())
                .build());

        assertThat(textoDe(pdf))
                .contains("IPCA + 6,20% a.a.")
                .contains("77,50% do rendimento bruto")
                .contains(RelatorioAtivoBuilder.NOTA_RENTABILIDADE_TAXA_SOMADA);
    }

    @Test
    @DisplayName("construir (fixa) - título isento deve indicar a isenção e usar a nota própria")
    void construirFixa_tituloIsento_deveIndicarIsencao() {
        byte[] pdf = relatorioAtivoBuilder.construir(DadosRelatorioAtivoFixo.builder()
                .nomeUsuario("Lucas Silva")
                .geradoEm(GERADO_EM)
                .perfil(perfil(false))
                .titulo(tituloCdb()
                        .isentoIr(true)
                        .rentabilidades(List.of(new RentabilidadePrazoDTO(
                                "180 dias", BigDecimal.ZERO, new BigDecimal("110"), new BigDecimal("110.00"))))
                        .build())
                .build());

        assertThat(textoDe(pdf))
                .contains("Imposto de Renda Isento")
                .contains(RelatorioAtivoBuilder.NOTA_ISENTO_IR)
                .contains(RelatorioAtivoBuilder.PERFIL_INCOMPLETO)
                .doesNotContain(RelatorioAtivoBuilder.NOTA_RENTABILIDADE);
    }
}
