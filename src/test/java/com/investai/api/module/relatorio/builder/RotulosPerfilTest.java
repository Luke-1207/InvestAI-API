package com.investai.api.module.relatorio.builder;

import com.investai.api.module.perfil.dto.ValorDescritoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RotulosPerfilTest {

    @Test
    @DisplayName("de - deve traduzir o valor do enum para o rótulo legível")
    void de_deveTraduzirValor() {
        assertThat(RotulosPerfil.de(ValorDescritoDTO.builder().valor("PRESERVAR_CAPITAL").build())).isEqualTo("Preservar capital");
        assertThat(RotulosPerfil.de(ValorDescritoDTO.builder().valor("MEDIO_PRAZO").build())).isEqualTo("Médio prazo (1 a 5 anos)");
    }

    @Test
    @DisplayName("de - valor desconhecido deve ser devolvido como veio, e ausente deve ser null")
    void de_valorDesconhecidoOuAusente() {
        assertThat(RotulosPerfil.de(ValorDescritoDTO.builder().valor("OUTRO").build())).isEqualTo("OUTRO");
        assertThat(RotulosPerfil.de(ValorDescritoDTO.builder().build())).isNull();
        assertThat(RotulosPerfil.de(null)).isNull();
    }
}
