package com.investai.api.module.relatorio.builder;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.relatorio.component.PdfDisclaimerComponent;
import com.investai.api.module.relatorio.component.PdfEstilo;
import com.investai.api.module.relatorio.component.PdfHeaderComponent;
import com.investai.api.module.relatorio.component.PdfSecaoComponent;
import com.investai.api.module.relatorio.component.PdfTableComponent;
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoFixo;
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoVariavel;
import com.investai.api.module.relatorio.dto.TituloRendaFixaRelatorio;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RelatorioAtivoBuilder {

    public static final String RESUMO_INDISPONIVEL = "Análise indisponível no momento.";
    public static final String PERFIL_INCOMPLETO =
            "Você ainda não respondeu o quiz de perfil. Complete o perfil para ver a compatibilidade deste ativo.";
    public static final String COMPATIBILIDADE_INDISPONIVEL =
            "Não foi possível calcular a compatibilidade agora, porque o ativo está sem cotação disponível.";
    public static final String COTACAO_INDISPONIVEL = "Cotação indisponível no momento.";
    public static final String NOTA_RENTABILIDADE =
            "A taxa líquida aplica a alíquota de IR regressivo de cada prazo sobre a taxa bruta contratada e fica "
                    + "na mesma unidade dela. É uma estimativa e não considera IOF nem taxas da instituição.";
    public static final String NOTA_RENTABILIDADE_TAXA_SOMADA =
            "Neste título a taxa é somada a um índice (IPCA ou Selic), e o IR regressivo incide sobre o rendimento "
                    + "total, não só sobre a taxa fixa. Por isso a última coluna mostra a parcela do rendimento que "
                    + "fica com o investidor em cada prazo. É uma estimativa e não considera IOF nem taxas da instituição.";
    public static final String NOTA_ISENTO_IR =
            "Este título é isento de Imposto de Renda para pessoa física, então a taxa líquida é igual à bruta.";

    private static final BigDecimal PERCENTUAL_TOTAL = BigDecimal.valueOf(100);

    private static final Map<TipoAtivo, String> ROTULOS_TIPO = Map.of(
            TipoAtivo.ACAO, "Ação",
            TipoAtivo.FII, "Fundo imobiliário",
            TipoAtivo.ETF, "ETF");

    private static final Map<String, String> ROTULOS_PERFIL = Map.of(
            "CONSERVADOR", "Conservador",
            "MODERADO", "Moderado",
            "ARROJADO", "Arrojado",
            "RENDA_PASSIVA", "Renda passiva",
            "CRESCIMENTO_PATRIMONIO", "Crescimento de patrimônio",
            "PRESERVAR_CAPITAL", "Preservar capital",
            "CURTO_PRAZO", "Curto prazo (menos de 1 ano)",
            "MEDIO_PRAZO", "Médio prazo (1 a 5 anos)",
            "LONGO_PRAZO", "Longo prazo (mais de 5 anos)");

    private static final Map<Compatibilidade, String> ROTULOS_COMPATIBILIDADE = Map.of(
            Compatibilidade.ALTA, "Alta",
            Compatibilidade.MEDIA, "Média",
            Compatibilidade.BAIXA, "Baixa");

    private static final Map<Indexador, String> ROTULOS_INDEXADOR = Map.of(
            Indexador.SELIC, "Selic (pós-fixado)",
            Indexador.CDI, "CDI (pós-fixado)",
            Indexador.IPCA, "IPCA (inflação + taxa fixa)",
            Indexador.PREFIXADO, "Prefixado");

    private final PdfHeaderComponent pdfHeaderComponent;
    private final PdfTableComponent pdfTableComponent;
    private final PdfSecaoComponent pdfSecaoComponent;
    private final PdfDisclaimerComponent pdfDisclaimerComponent;

    public byte[] construir(DadosRelatorioAtivoVariavel dados) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        AcaoDetalheResponseDTO ativo = dados.ativo();

        try (Document documento = new Document(new PdfDocument(new PdfWriter(saida)))) {
            pdfHeaderComponent.adicionar(documento, "Análise de ativo: " + ativo.getCodigo(),
                    dados.nomeUsuario(), dados.geradoEm());

            adicionarIdentificacao(documento, ativo);
            adicionarPerfil(documento, dados.perfil());
            adicionarIndicadores(documento, ativo);
            adicionarCompatibilidade(documento, dados);
            adicionarResumoIa(documento, dados.resumoIa());

            pdfDisclaimerComponent.adicionar(documento);
        }

        return saida.toByteArray();
    }

    public byte[] construir(DadosRelatorioAtivoFixo dados) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        TituloRendaFixaRelatorio titulo = dados.titulo();

        try (Document documento = new Document(new PdfDocument(new PdfWriter(saida)))) {
            pdfHeaderComponent.adicionar(documento, "Análise de título de renda fixa",
                    dados.nomeUsuario(), dados.geradoEm());

            adicionarIdentificacao(documento, titulo.nome(), titulo.categoria());
            adicionarPerfil(documento, dados.perfil());
            adicionarDadosDoTitulo(documento, titulo);
            adicionarRentabilidade(documento, titulo);

            pdfDisclaimerComponent.adicionar(documento);
        }

        return saida.toByteArray();
    }

    private void adicionarDadosDoTitulo(Document documento, TituloRendaFixaRelatorio titulo) {
        pdfSecaoComponent.adicionarTitulo(documento, "Dados do título");

        documento.add(pdfTableComponent.criar(
                new float[]{4, 6},
                List.of("Característica", "Valor"),
                List.of(
                        linha("Indexador", ROTULOS_INDEXADOR.get(titulo.indexador())),
                        linha("Taxa", taxa(titulo, titulo.taxa())),
                        linha("Vencimento", FormatoRelatorio.data(titulo.vencimento())),
                        linha("Investimento mínimo", FormatoRelatorio.moeda(titulo.investimentoMinimo())),
                        linha("Liquidez", titulo.liquidez()),
                        linha("Garantia", titulo.garantia()),
                        linha("Imposto de Renda", titulo.isentoIr() ? "Isento" : "Tributado (tabela regressiva)"))));
    }

    private void adicionarRentabilidade(Document documento, TituloRendaFixaRelatorio titulo) {
        pdfSecaoComponent.adicionarTitulo(documento, "Rentabilidade estimada por prazo");

        documento.add(pdfTableComponent.criar(
                new float[]{3, 2, 3, 4},
                List.of("Prazo", "IR", "Taxa bruta", "Líquido estimado"),
                titulo.rentabilidades().stream()
                        .map(r -> List.of(
                                r.prazo(),
                                FormatoRelatorio.percentual(r.aliquotaIR()),
                                taxa(titulo, r.taxaBruta()),
                                r.taxaLiquida() != null
                                        ? taxa(titulo, r.taxaLiquida())
                                        : FormatoRelatorio.percentual(PERCENTUAL_TOTAL.subtract(r.aliquotaIR()))
                                                + " do rendimento bruto"))
                        .toList()));

        pdfSecaoComponent.adicionarNota(documento, notaRentabilidade(titulo));
    }

    private void adicionarIdentificacao(Document documento, AcaoDetalheResponseDTO ativo) {
        adicionarIdentificacao(documento, ativo.getNome(), ROTULOS_TIPO.get(ativo.getTipo()) + " · " + ativo.getSetor());
    }

    private void adicionarIdentificacao(Document documento, String nome, String subtitulo) {
        documento.add(new Paragraph(nome)
                .setFont(PdfEstilo.fonteNegrito())
                .setFontSize(16)
                .setFontColor(PdfEstilo.COR_TEXTO)
                .setMargin(0));
        documento.add(new Paragraph(subtitulo)
                .setFont(PdfEstilo.fonteNormal())
                .setFontSize(PdfEstilo.TAMANHO_TEXTO)
                .setFontColor(PdfEstilo.COR_TEXTO_SECUNDARIO)
                .setMarginTop(0)
                .setMarginBottom(16));
    }

    private void adicionarPerfil(Document documento, PerfilResponseDTO perfil) {
        pdfSecaoComponent.adicionarTitulo(documento, "Seu perfil de investidor");

        if (perfil == null || !perfil.isPerfilPreenchido()) {
            pdfSecaoComponent.adicionarNota(documento, PERFIL_INCOMPLETO);
            return;
        }

        documento.add(pdfTableComponent.criar(
                new float[]{3, 7},
                List.of("Característica", "Seu perfil"),
                List.of(
                        linha("Perfil de risco", rotuloPerfil(perfil.getPerfilRisco().getValor())),
                        linha("Objetivo", rotuloPerfil(perfil.getObjetivoFinanceiro().getValor())),
                        linha("Horizonte", rotuloPerfil(perfil.getHorizonteInvestimento().getValor())),
                        linha("Valor disponível", FormatoRelatorio.moeda(perfil.getValorDisponivel())))));
    }

    private void adicionarIndicadores(Document documento, AcaoDetalheResponseDTO ativo) {
        pdfSecaoComponent.adicionarTitulo(documento, "Indicadores do ativo");

        documento.add(pdfTableComponent.criar(
                new float[]{5, 5},
                List.of("Indicador", "Valor"),
                List.of(
                        linha("Preço", FormatoRelatorio.moeda(ativo.getPreco())),
                        linha("Variação no dia", FormatoRelatorio.percentualComSinal(ativo.getVariacaoPercentual())),
                        linha("Dividend Yield (DY)", FormatoRelatorio.percentual(ativo.getDividendYield())),
                        linha("Preço/Lucro (P/L)", FormatoRelatorio.numero(ativo.getPrecoLucro())),
                        linha("Preço/Valor Patrimonial (P/VP)", FormatoRelatorio.numero(ativo.getPrecoValorPatrimonial())),
                        linha("Mínima em 52 semanas", FormatoRelatorio.moeda(ativo.getMinimo52Semanas())),
                        linha("Máxima em 52 semanas", FormatoRelatorio.moeda(ativo.getMaximo52Semanas())))));

        if (!ativo.isCotacaoDisponivel()) {
            pdfSecaoComponent.adicionarNota(documento, COTACAO_INDISPONIVEL);
        } else if (ativo.getCotacaoAtualizadaEm() != null) {
            String fonte = ativo.getFonteCotacao() == null ? "" : " Fonte: " + ativo.getFonteCotacao() + ".";
            pdfSecaoComponent.adicionarNota(documento,
                    "Cotação atualizada em " + FormatoRelatorio.dataHora(ativo.getCotacaoAtualizadaEm()) + "." + fonte);
        }
    }

    private void adicionarCompatibilidade(Document documento, DadosRelatorioAtivoVariavel dados) {
        pdfSecaoComponent.adicionarTitulo(documento, "Compatibilidade com o seu perfil");

        SugestaoAtivoItemDTO compatibilidade = dados.compatibilidade();
        if (compatibilidade == null) {
            boolean perfilPreenchido = dados.perfil() != null && dados.perfil().isPerfilPreenchido();
            pdfSecaoComponent.adicionarNota(documento,
                    perfilPreenchido ? COMPATIBILIDADE_INDISPONIVEL : PERFIL_INCOMPLETO);
            return;
        }

        documento.add(new Paragraph()
                .add(new Text("Score " + compatibilidade.getScore() + " de 100")
                        .setFont(PdfEstilo.fonteNegrito())
                        .setFontSize(14)
                        .setFontColor(PdfEstilo.COR_TEXTO))
                .add(new Text("   Compatibilidade " + ROTULOS_COMPATIBILIDADE.get(compatibilidade.getCompatibilidade()))
                        .setFont(PdfEstilo.fonteNegrito())
                        .setFontSize(PdfEstilo.TAMANHO_TEXTO)
                        .setFontColor(PdfEstilo.COR_TEXTO_SECUNDARIO))
                .setMarginTop(0)
                .setMarginBottom(4));

        String justificativa = compatibilidade.getJustificativa();
        if (justificativa != null && !justificativa.isBlank()) {
            pdfSecaoComponent.adicionarParagrafo(documento, justificativa);
        }
    }

    private void adicionarResumoIa(Document documento, String resumoIa) {
        pdfSecaoComponent.adicionarTitulo(documento, "Resumo da InvestAI");

        if (resumoIa == null || resumoIa.isBlank()) {
            pdfSecaoComponent.adicionarNota(documento, RESUMO_INDISPONIVEL);
            return;
        }
        pdfSecaoComponent.adicionarParagrafo(documento, resumoIa);
    }

    private String notaRentabilidade(TituloRendaFixaRelatorio titulo) {
        if (titulo.isentoIr()) {
            return NOTA_ISENTO_IR;
        }
        return titulo.taxaSomadaAoIndexador() ? NOTA_RENTABILIDADE_TAXA_SOMADA : NOTA_RENTABILIDADE;
    }

    private String taxa(TituloRendaFixaRelatorio titulo, BigDecimal valor) {
        return FormatoRelatorio.taxaComIndexador(titulo.indexador(), valor, titulo.taxaSomadaAoIndexador());
    }

    private String rotuloPerfil(String valor) {
        return valor == null ? null : ROTULOS_PERFIL.getOrDefault(valor, valor);
    }

    private List<String> linha(String rotulo, String valor) {
        return Arrays.asList(rotulo, valor);
    }
}
