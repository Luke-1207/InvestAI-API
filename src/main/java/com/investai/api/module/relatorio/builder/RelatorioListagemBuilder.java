package com.investai.api.module.relatorio.builder;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.relatorio.component.PdfDisclaimerComponent;
import com.investai.api.module.relatorio.component.PdfHeaderComponent;
import com.investai.api.module.relatorio.component.PdfSecaoComponent;
import com.investai.api.module.relatorio.component.PdfTableComponent;
import com.investai.api.module.relatorio.dto.DadosRelatorioListagem;
import com.investai.api.module.relatorio.dto.ItemListagemRelatorio;
import com.investai.api.module.relatorio.dto.ModuloRelatorio;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RelatorioListagemBuilder {

    public static final String SEM_FILTROS = "Nenhum filtro aplicado.";
    public static final int TAMANHO_MAXIMO_JUSTIFICATIVA = 140;

    private static final float TAMANHO_FONTE_TABELA = 8.5f;
    private static final String RETICENCIAS = "...";

    private static final Map<ModuloRelatorio, String> TITULOS = Map.of(
            ModuloRelatorio.VARIAVEL, "Listagem ranqueada: Renda Variável",
            ModuloRelatorio.FIXA, "Listagem ranqueada: Renda Fixa",
            ModuloRelatorio.AMBOS, "Listagem ranqueada: Renda Variável e Renda Fixa");

    private static final Map<Compatibilidade, String> ROTULOS_COMPATIBILIDADE = Map.of(
            Compatibilidade.ALTA, "Alta",
            Compatibilidade.MEDIA, "Média",
            Compatibilidade.BAIXA, "Baixa");

    private final PdfHeaderComponent pdfHeaderComponent;
    private final PdfTableComponent pdfTableComponent;
    private final PdfSecaoComponent pdfSecaoComponent;
    private final PdfDisclaimerComponent pdfDisclaimerComponent;

    public byte[] construir(DadosRelatorioListagem dados) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();

        try (Document documento = new Document(new PdfDocument(new PdfWriter(saida)), PageSize.A4.rotate())) {
            pdfHeaderComponent.adicionar(documento, TITULOS.get(dados.modulo()), dados.nomeUsuario(), dados.geradoEm());

            adicionarPerfil(documento, dados.perfil());
            adicionarFiltros(documento, dados.filtros());
            adicionarAtivos(documento, dados);

            pdfDisclaimerComponent.adicionar(documento);
        }

        return saida.toByteArray();
    }

    private void adicionarPerfil(Document documento, PerfilResponseDTO perfil) {
        pdfSecaoComponent.adicionarTitulo(documento, "Seu perfil de investidor");

        documento.add(pdfTableComponent.criar(
                new float[]{2, 3, 3, 2},
                List.of("Perfil de risco", "Objetivo", "Horizonte", "Valor disponível"),
                List.of(Arrays.asList(
                        RotulosPerfil.de(perfil.getPerfilRisco()),
                        RotulosPerfil.de(perfil.getObjetivoFinanceiro()),
                        RotulosPerfil.de(perfil.getHorizonteInvestimento()),
                        FormatoRelatorio.moeda(perfil.getValorDisponivel())))));
    }

    private void adicionarFiltros(Document documento, Map<String, String> filtros) {
        pdfSecaoComponent.adicionarTitulo(documento, "Filtros utilizados");

        if (filtros == null || filtros.isEmpty()) {
            pdfSecaoComponent.adicionarNota(documento, SEM_FILTROS);
            return;
        }

        documento.add(pdfTableComponent.criar(
                new float[]{3, 7},
                List.of("Filtro", "Valor"),
                filtros.entrySet().stream()
                        .map(filtro -> Arrays.asList(filtro.getKey(), filtro.getValue()))
                        .toList()));
    }

    private void adicionarAtivos(Document documento, DadosRelatorioListagem dados) {
        pdfSecaoComponent.adicionarTitulo(documento, "Ativos ranqueados");

        documento.add(pdfTableComponent.criar(
                new float[]{1.3f, 3f, 1.1f, 2.2f, 0.9f, 1.5f, 5f},
                List.of("Código", "Nome", "Tipo", "Dado principal", "Score", "Compatibilidade", "Justificativa"),
                dados.itens().stream().map(this::linha).toList(),
                TAMANHO_FONTE_TABELA));

        if (dados.totalNaListagem() > dados.itens().size()) {
            pdfSecaoComponent.adicionarNota(documento, "Mostrando os " + dados.itens().size()
                    + " ativos mais bem pontuados de " + dados.totalNaListagem() + " na listagem.");
        }
        if (dados.ativosNaoEncontrados() != null && !dados.ativosNaoEncontrados().isEmpty()) {
            pdfSecaoComponent.adicionarNota(documento, "Não encontrados na listagem ranqueada: "
                    + String.join(", ", dados.ativosNaoEncontrados()) + ".");
        }
    }

    private List<String> linha(ItemListagemRelatorio item) {
        return Arrays.asList(
                item.codigo(),
                item.nome(),
                item.tipo(),
                item.dadoPrincipal(),
                item.score() == null ? null : String.valueOf(item.score()),
                item.compatibilidade() == null ? null : ROTULOS_COMPATIBILIDADE.get(item.compatibilidade()),
                resumir(item.justificativa()));
    }

    private String resumir(String justificativa) {
        if (justificativa == null || justificativa.length() <= TAMANHO_MAXIMO_JUSTIFICATIVA) {
            return justificativa;
        }
        String corte = justificativa.substring(0, TAMANHO_MAXIMO_JUSTIFICATIVA - RETICENCIAS.length());
        int ultimoEspaco = corte.lastIndexOf(' ');
        if (ultimoEspaco > TAMANHO_MAXIMO_JUSTIFICATIVA / 2) {
            corte = corte.substring(0, ultimoEspaco);
        }
        return corte.trim() + RETICENCIAS;
    }
}
