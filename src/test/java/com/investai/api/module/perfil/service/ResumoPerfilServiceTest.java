package com.investai.api.module.perfil.service;

import com.investai.api.module.perfil.entity.PerfilInvestidor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumoPerfilServiceTest {

    private final ResumoPerfilService resumoPerfilService = new ResumoPerfilService();

    @Test
    @DisplayName("gerarResumoIA - deve montar frase combinando objetivo, horizonte e perfil de risco")
    void gerarResumoIA_deveMontarFraseComObjetivoHorizonteEPerfilRisco() {
        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .objetivo("CRESCIMENTO_PATRIMONIO")
                .horizonte("LONGO_PRAZO")
                .perfilRisco("MODERADO")
                .build();

        String resumo = resumoPerfilService.gerarResumoIA(perfil);

        assertThat(resumo).isEqualTo("Você busca crescimento do patrimônio no longo prazo, com perfil moderado.");
    }

    @Test
    @DisplayName("gerarResumoIA - deve montar frase para outra combinação (renda passiva, curto prazo, conservador)")
    void gerarResumoIA_deveMontarFraseParaOutraCombinacao() {
        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .objetivo("RENDA_PASSIVA")
                .horizonte("CURTO_PRAZO")
                .perfilRisco("CONSERVADOR")
                .build();

        String resumo = resumoPerfilService.gerarResumoIA(perfil);

        assertThat(resumo).isEqualTo("Você busca renda passiva no curto prazo, com perfil conservador.");
    }

    @Test
    @DisplayName("gerarResumoIA - deve montar frase para combinação restante (preservar capital, médio prazo, arrojado)")
    void gerarResumoIA_deveMontarFraseParaCombinacaoRestante() {
        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .objetivo("PRESERVAR_CAPITAL")
                .horizonte("MEDIO_PRAZO")
                .perfilRisco("ARROJADO")
                .build();

        String resumo = resumoPerfilService.gerarResumoIA(perfil);

        assertThat(resumo).isEqualTo("Você busca preservar o capital no médio prazo, com perfil arrojado.");
    }

    @Test
    @DisplayName("gerarResumoIA - deve retornar mensagem padrão quando o perfil ainda não foi preenchido")
    void gerarResumoIA_deveRetornarMensagemPadraoQuandoPerfilNaoPreenchido() {
        PerfilInvestidor perfil = PerfilInvestidor.builder().build();

        String resumo = resumoPerfilService.gerarResumoIA(perfil);

        assertThat(resumo).isEqualTo("Complete o quiz para receber sua análise personalizada.");
    }

    @Test
    @DisplayName("gerarResumoIA - deve retornar mensagem padrão quando só parte do perfil está preenchida")
    void gerarResumoIA_deveRetornarMensagemPadraoQuandoParcialmentePreenchido() {
        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .objetivo("CRESCIMENTO_PATRIMONIO")
                .horizonte("LONGO_PRAZO")
                .perfilRisco(null)
                .build();

        String resumo = resumoPerfilService.gerarResumoIA(perfil);

        assertThat(resumo).isEqualTo("Complete o quiz para receber sua análise personalizada.");
    }
}