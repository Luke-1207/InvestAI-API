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
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoVariavel;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
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

    private void adicionarIdentificacao(Document documento, AcaoDetalheResponseDTO ativo) {
        documento.add(new Paragraph(ativo.getNome())
                .setFont(PdfEstilo.fonteNegrito())
                .setFontSize(16)
                .setFontColor(PdfEstilo.COR_TEXTO)
                .setMargin(0));
        documento.add(new Paragraph(ROTULOS_TIPO.get(ativo.getTipo()) + " · " + ativo.getSetor())
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

    private String rotuloPerfil(String valor) {
        return valor == null ? null : ROTULOS_PERFIL.getOrDefault(valor, valor);
    }

    private List<String> linha(String rotulo, String valor) {
        return Arrays.asList(rotulo, valor);
    }
}
