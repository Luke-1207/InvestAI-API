package com.investai.api.infra.rabbitmq;

import com.investai.api.infra.rabbitmq.dto.PerfilIaDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PerfilRisco;
import com.investai.api.module.perfil.entity.PreferenciaSetor;
import com.investai.api.module.perfil.entity.SetorPreferido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PerfilIaMapperTest {

    @Test
    @DisplayName("dePerfil - deve converter enums, tipos aceitos e setores")
    void dePerfil_deveConverterTodosOsCampos() {
        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .perfilRisco("ARROJADO")
                .horizonte("CURTO_PRAZO")
                .objetivo("RENDA_PASSIVA")
                .valorDisponivel(new BigDecimal("5000"))
                .tiposAceitos(List.of("ACAO", "FII"))
                .setoresPreferidos(List.of(new SetorPreferido("Energia", PreferenciaSetor.PREFERIR)))
                .build();

        PerfilIaDTO resultado = PerfilIaMapper.dePerfil(perfil);

        assertThat(resultado.getPerfilRisco()).isEqualTo(PerfilRisco.ARROJADO);
        assertThat(resultado.getHorizonte()).isEqualTo(HorizonteInvestimento.CURTO_PRAZO);
        assertThat(resultado.getObjetivo()).isEqualTo(ObjetivoFinanceiro.RENDA_PASSIVA);
        assertThat(resultado.getValorDisponivel()).isEqualByComparingTo("5000");
        assertThat(resultado.getTiposAceitos()).containsExactly(TipoAtivo.ACAO, TipoAtivo.FII);
        assertThat(resultado.getSetoresPreferidos()).singleElement().satisfies(setor -> {
            assertThat(setor.getSetor()).isEqualTo("Energia");
            assertThat(setor.getPreferencia()).isEqualTo(PreferenciaSetor.PREFERIR);
        });
    }

    @Test
    @DisplayName("dePerfil - perfil vazio deve gerar listas vazias e enums nulos")
    void dePerfil_perfilVazio_deveGerarListasVazias() {
        PerfilIaDTO resultado = PerfilIaMapper.dePerfil(new PerfilInvestidor());

        assertThat(resultado.getPerfilRisco()).isNull();
        assertThat(resultado.getHorizonte()).isNull();
        assertThat(resultado.getObjetivo()).isNull();
        assertThat(resultado.getTiposAceitos()).isEmpty();
        assertThat(resultado.getSetoresPreferidos()).isEmpty();
    }
}
