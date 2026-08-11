package com.investai.api.module.ativo.dto;

import com.investai.api.infra.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoHistoricoTest {

    @Test
    @DisplayName("fromCodigo - deve resolver todos os códigos válidos com os dias corretos")
    void fromCodigo_deveResolverTodosOsCodigosValidos() {
        assertThat(PeriodoHistorico.fromCodigo("1S")).isEqualTo(PeriodoHistorico.UMA_SEMANA);
        assertThat(PeriodoHistorico.fromCodigo("1S").getDiasAtras()).isEqualTo(7);

        assertThat(PeriodoHistorico.fromCodigo("1M")).isEqualTo(PeriodoHistorico.UM_MES);
        assertThat(PeriodoHistorico.fromCodigo("1M").getDiasAtras()).isEqualTo(30);

        assertThat(PeriodoHistorico.fromCodigo("3M")).isEqualTo(PeriodoHistorico.TRES_MESES);
        assertThat(PeriodoHistorico.fromCodigo("3M").getDiasAtras()).isEqualTo(90);

        assertThat(PeriodoHistorico.fromCodigo("6M")).isEqualTo(PeriodoHistorico.SEIS_MESES);
        assertThat(PeriodoHistorico.fromCodigo("6M").getDiasAtras()).isEqualTo(180);

        assertThat(PeriodoHistorico.fromCodigo("1A")).isEqualTo(PeriodoHistorico.UM_ANO);
        assertThat(PeriodoHistorico.fromCodigo("1A").getDiasAtras()).isEqualTo(365);
    }

    @Test
    @DisplayName("fromCodigo - deve ser case insensitive")
    void fromCodigo_deveSerCaseInsensitive() {
        assertThat(PeriodoHistorico.fromCodigo("1m")).isEqualTo(PeriodoHistorico.UM_MES);
    }

    @Test
    @DisplayName("fromCodigo - deve lançar BusinessException para código inválido")
    void fromCodigo_deveLancarExcecaoParaCodigoInvalido() {
        assertThatThrownBy(() -> PeriodoHistorico.fromCodigo("2Y"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Período inválido: 2Y");
    }
}