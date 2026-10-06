package com.investai.api.module.relatorio.builder;

import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.dto.DescricoesPerfil;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.dto.ValorDescritoDTO;
import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilRisco;
import com.investai.api.module.perfil.entity.PreferenciaSetor;
import com.investai.api.module.perfil.entity.SetorPreferido;
import com.investai.api.module.relatorio.PdfTesteUtil;
import com.investai.api.module.relatorio.component.PdfDisclaimerComponent;
import com.investai.api.module.relatorio.component.PdfHeaderComponent;
import com.investai.api.module.relatorio.component.PdfSecaoComponent;
import com.investai.api.module.relatorio.component.PdfTableComponent;
import com.investai.api.module.relatorio.dto.DadosRelatorioPerfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioPerfilBuilderTest {

    private final RelatorioPerfilBuilder relatorioPerfilBuilder = new RelatorioPerfilBuilder(
            new PdfHeaderComponent(), new PdfTableComponent(), new PdfSecaoComponent(), new PdfDisclaimerComponent());

    private PerfilResponseDTO.PerfilResponseDTOBuilder perfil() {
        return PerfilResponseDTO.builder()
                .perfilPreenchido(true)
                .perfilRisco(ValorDescritoDTO.builder()
                        .valor("MODERADO").descricao(DescricoesPerfil.PERFIL_RISCO.get(PerfilRisco.MODERADO)).build())
                .objetivoFinanceiro(ValorDescritoDTO.builder()
                        .valor("RENDA_PASSIVA").descricao(DescricoesPerfil.OBJETIVO_FINANCEIRO.get(ObjetivoFinanceiro.RENDA_PASSIVA)).build())
                .horizonteInvestimento(ValorDescritoDTO.builder()
                        .valor("LONGO_PRAZO").descricao(DescricoesPerfil.HORIZONTE_INVESTIMENTO.get(HorizonteInvestimento.LONGO_PRAZO)).build())
                .valorDisponivel(new BigDecimal("15000"))
                .tiposAceitos(List.of(TipoAtivo.ACAO, TipoAtivo.FII))
                .setoresPreferidos(List.of(
                        new SetorPreferido("Energia Elétrica", PreferenciaSetor.PREFERIR),
                        new SetorPreferido("Bancos", PreferenciaSetor.PREFERIR),
                        new SetorPreferido("Varejo", PreferenciaSetor.EVITAR)))
                .resumoIA("Você busca renda com risco moderado no longo prazo.")
                .atualizadoEm(LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    private DadosRelatorioPerfil.DadosRelatorioPerfilBuilder dados() {
        return DadosRelatorioPerfil.builder()
                .nomeUsuario("Lucas Silva")
                .email("lucas@email.com")
                .telefone("19999998888")
                .cadastradoEm(LocalDateTime.of(2026, 3, 15, 9, 30))
                .geradoEm(LocalDateTime.of(2026, 10, 6, 15, 20))
                .perfil(perfil().build());
    }

    private String textoDe(byte[] pdf) {
        return PdfTesteUtil.extrairTexto(pdf).replaceAll("\\s+", " ");
    }

    @Test
    @DisplayName("construir - deve incluir dados cadastrais, perfil com descrições, tipos aceitos, setores e aviso")
    void construir_deveIncluirTodasAsSecoes() throws IOException {
        byte[] pdf = relatorioPerfilBuilder.construir(dados().build());
        Path arquivo = Path.of("target", "pdf-teste", "perfil-investidor.pdf");
        Files.createDirectories(arquivo.getParent());
        Files.write(arquivo, pdf);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(textoDe(pdf))
                .contains("Perfil do investidor", "Gerado para Lucas Silva", "Em 06/10/2026 às 15:20")
                .contains("Nome Lucas Silva", "lucas@email.com", "(19) 99999-8888", "15/03/2026 às 09:30")
                .contains("Moderado", "aceita alguma volatilidade em busca de crescimento")
                .contains("Renda passiva", "recebendo retornos periódicos")
                .contains("Longo prazo (mais de 5 anos)", "R$ 15.000,00")
                .contains("Você busca renda com risco moderado no longo prazo.")
                .contains("Perfil atualizado em 01/10/2026 às 10:00.")
                .contains("Ações", "Você vira sócio", "Fundos imobiliários (FIIs)", "rendimentos todo mês")
                .contains("Setores que você prefere Energia Elétrica, Bancos")
                .contains("Setores que você evita Varejo")
                .contains(PdfDisclaimerComponent.TITULO)
                .doesNotContain("acompanham um índice")
                .doesNotContain("Você ainda não respondeu o quiz");
    }

    @Test
    @DisplayName("construir - perfil não preenchido deve mostrar só os dados cadastrais e a orientação do quiz")
    void construir_perfilNaoPreenchido_deveOrientarAResponderOQuiz() {
        byte[] pdf = relatorioPerfilBuilder.construir(dados().perfil(perfil().perfilPreenchido(false).build()).build());

        assertThat(textoDe(pdf))
                .contains("Nome Lucas Silva")
                .contains("Você ainda não respondeu o quiz de perfil.")
                .contains(PdfDisclaimerComponent.TITULO)
                .doesNotContain("Tipos de ativo que você aceita")
                .doesNotContain("Setores que você prefere")
                .doesNotContain("Moderado");
    }

    @Test
    @DisplayName("construir - sem telefone, tipos aceitos nem setores deve mostrar traço e as notas de vazio")
    void construir_semDadosOpcionais_deveMostrarTracoENotas() {
        byte[] pdf = relatorioPerfilBuilder.construir(dados()
                .telefone(null)
                .perfil(perfil().tiposAceitos(List.of()).setoresPreferidos(List.of()).resumoIA(null).atualizadoEm(null).build())
                .build());

        assertThat(textoDe(pdf))
                .contains("Telefone -")
                .contains(RelatorioPerfilBuilder.SEM_TIPOS_ACEITOS)
                .contains(RelatorioPerfilBuilder.SEM_SETORES)
                .doesNotContain("Perfil atualizado em");
    }

    @Test
    @DisplayName("construir - só setores preferidos deve indicar Nenhum nos evitados")
    void construir_soSetoresPreferidos_deveIndicarNenhumNosEvitados() {
        byte[] pdf = relatorioPerfilBuilder.construir(dados()
                .perfil(perfil()
                        .tiposAceitos(List.of(TipoAtivo.ETF))
                        .setoresPreferidos(List.of(new SetorPreferido("Saúde", PreferenciaSetor.PREFERIR)))
                        .build())
                .build());

        assertThat(textoDe(pdf))
                .contains("Setores que você prefere Saúde")
                .contains("Setores que você evita " + RelatorioPerfilBuilder.NENHUM)
                .contains("ETFs", "acompanham um índice");
    }
}
