package com.investai.api.module.relatorio.builder;

import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.dto.ValorDescritoDTO;
import com.investai.api.module.perfil.entity.PreferenciaSetor;
import com.investai.api.module.perfil.entity.SetorPreferido;
import com.investai.api.module.relatorio.component.PdfDisclaimerComponent;
import com.investai.api.module.relatorio.component.PdfHeaderComponent;
import com.investai.api.module.relatorio.component.PdfSecaoComponent;
import com.investai.api.module.relatorio.component.PdfTableComponent;
import com.investai.api.module.relatorio.dto.DadosRelatorioPerfil;
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
public class RelatorioPerfilBuilder {

    public static final String PERFIL_INCOMPLETO =
            "Você ainda não respondeu o quiz de perfil. Responda o quiz no aplicativo para que este relatório "
                    + "mostre seu perfil de risco, objetivo, horizonte e preferências.";
    public static final String SEM_TIPOS_ACEITOS = "Nenhum tipo de ativo selecionado.";
    public static final String SEM_SETORES = "Nenhuma preferência de setor informada.";
    public static final String NENHUM = "Nenhum";

    private static final Map<TipoAtivo, String> ROTULOS_TIPO = Map.of(
            TipoAtivo.ACAO, "Ações",
            TipoAtivo.FII, "Fundos imobiliários (FIIs)",
            TipoAtivo.ETF, "ETFs");

    private static final Map<TipoAtivo, String> DESCRICOES_TIPO = Map.of(
            TipoAtivo.ACAO, "Pequenas partes de uma empresa negociadas na bolsa. Você vira sócio e pode ganhar com a "
                    + "valorização e com os dividendos, mas o preço oscila bastante.",
            TipoAtivo.FII, "Fundos que investem em imóveis ou em títulos do setor imobiliário. Costumam distribuir "
                    + "rendimentos todo mês, com cotas negociadas na bolsa.",
            TipoAtivo.ETF, "Fundos que acompanham um índice, como o Ibovespa. Com uma única cota você investe em "
                    + "várias empresas ao mesmo tempo, o que ajuda a diversificar.");

    private final PdfHeaderComponent pdfHeaderComponent;
    private final PdfTableComponent pdfTableComponent;
    private final PdfSecaoComponent pdfSecaoComponent;
    private final PdfDisclaimerComponent pdfDisclaimerComponent;

    public byte[] construir(DadosRelatorioPerfil dados) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();

        try (Document documento = new Document(new PdfDocument(new PdfWriter(saida)))) {
            pdfHeaderComponent.adicionar(documento, "Perfil do investidor", dados.nomeUsuario(), dados.geradoEm());

            adicionarDadosCadastrais(documento, dados);

            PerfilResponseDTO perfil = dados.perfil();
            if (perfil == null || !perfil.isPerfilPreenchido()) {
                pdfSecaoComponent.adicionarTitulo(documento, "Seu perfil de investidor");
                pdfSecaoComponent.adicionarNota(documento, PERFIL_INCOMPLETO);
            } else {
                adicionarPerfil(documento, perfil);
                adicionarTiposAceitos(documento, perfil.getTiposAceitos());
                adicionarSetores(documento, perfil.getSetoresPreferidos());
            }

            pdfDisclaimerComponent.adicionar(documento);
        }

        return saida.toByteArray();
    }

    private void adicionarDadosCadastrais(Document documento, DadosRelatorioPerfil dados) {
        pdfSecaoComponent.adicionarTitulo(documento, "Dados cadastrais");

        documento.add(pdfTableComponent.criar(
                new float[]{3, 7},
                List.of("Dado", "Valor"),
                List.of(
                        Arrays.asList("Nome", dados.nomeUsuario()),
                        Arrays.asList("E-mail", dados.email()),
                        Arrays.asList("Telefone", FormatoRelatorio.telefone(dados.telefone())),
                        Arrays.asList("Cadastro na InvestAI", FormatoRelatorio.dataHora(dados.cadastradoEm())))));
    }

    private void adicionarPerfil(Document documento, PerfilResponseDTO perfil) {
        pdfSecaoComponent.adicionarTitulo(documento, "Seu perfil de investidor");

        documento.add(pdfTableComponent.criar(
                new float[]{2f, 3.3f, 4.7f},
                List.of("Característica", "Seu perfil", "O que isso significa"),
                List.of(
                        linhaDescrita("Perfil de risco", perfil.getPerfilRisco()),
                        linhaDescrita("Objetivo", perfil.getObjetivoFinanceiro()),
                        linhaDescrita("Horizonte", perfil.getHorizonteInvestimento()),
                        Arrays.asList("Valor disponível", FormatoRelatorio.moeda(perfil.getValorDisponivel()),
                                "Quanto você informou ter disponível para investir. É usado para sugerir ativos "
                                        + "que cabem no seu bolso."))));

        if (perfil.getResumoIA() != null && !perfil.getResumoIA().isBlank()) {
            pdfSecaoComponent.adicionarParagrafo(documento, perfil.getResumoIA());
        }
        if (perfil.getAtualizadoEm() != null) {
            pdfSecaoComponent.adicionarNota(documento,
                    "Perfil atualizado em " + FormatoRelatorio.dataHora(perfil.getAtualizadoEm()) + ".");
        }
    }

    private void adicionarTiposAceitos(Document documento, List<TipoAtivo> tiposAceitos) {
        pdfSecaoComponent.adicionarTitulo(documento, "Tipos de ativo que você aceita");

        if (tiposAceitos == null || tiposAceitos.isEmpty()) {
            pdfSecaoComponent.adicionarNota(documento, SEM_TIPOS_ACEITOS);
            return;
        }

        documento.add(pdfTableComponent.criar(
                new float[]{3, 7},
                List.of("Tipo", "O que é"),
                tiposAceitos.stream()
                        .map(tipo -> List.of(ROTULOS_TIPO.get(tipo), DESCRICOES_TIPO.get(tipo)))
                        .toList()));
    }

    private void adicionarSetores(Document documento, List<SetorPreferido> setores) {
        pdfSecaoComponent.adicionarTitulo(documento, "Setores");

        if (setores == null || setores.isEmpty()) {
            pdfSecaoComponent.adicionarNota(documento, SEM_SETORES);
            return;
        }

        documento.add(pdfTableComponent.criar(
                new float[]{3, 7},
                List.of("Preferência", "Setores"),
                List.of(
                        List.of("Setores que você prefere", setoresCom(setores, PreferenciaSetor.PREFERIR)),
                        List.of("Setores que você evita", setoresCom(setores, PreferenciaSetor.EVITAR)))));
    }

    private String setoresCom(List<SetorPreferido> setores, PreferenciaSetor preferencia) {
        List<String> nomes = setores.stream()
                .filter(setor -> setor.getPreferencia() == preferencia)
                .map(SetorPreferido::getSetor)
                .toList();
        return nomes.isEmpty() ? NENHUM : String.join(", ", nomes);
    }

    private List<String> linhaDescrita(String rotulo, ValorDescritoDTO campo) {
        return Arrays.asList(rotulo, RotulosPerfil.de(campo), campo == null ? null : campo.getDescricao());
    }
}
