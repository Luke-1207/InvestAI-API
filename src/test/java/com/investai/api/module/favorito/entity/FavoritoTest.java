package com.investai.api.module.favorito.entity;

import com.investai.api.module.notificacao.entity.TipoNotificacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FavoritoTest {

    private Favorito.FavoritoBuilder favorito() {
        return Favorito.builder().usuarioId(UUID.randomUUID()).ativoId(UUID.randomUUID()).tipoAtivo(TipoAtivoFavorito.ACAO_FII_ETF);
    }

    @Test
    @DisplayName("builder - deve nascer com alertas ligados, todos os tipos e limiar de 5%")
    void builder_deveAplicarOsPadroes() {
        Favorito novo = favorito().build();

        assertThat(novo.isAlertasAtivos()).isTrue();
        assertThat(novo.getTiposAlertaAtivos()).isNull();
        assertThat(novo.getLimiarVariacao()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("onCreate - deve registrar a data de criação ao persistir")
    void onCreate_devePreencherCriadoEm() {
        Favorito novo = favorito().build();

        novo.onCreate();

        assertThat(novo.getCriadoEm()).isNotNull();
    }

    @Test
    @DisplayName("onCreate - não deve sobrescrever uma data de criação já informada")
    void onCreate_devePreservarCriadoEmInformado() {
        LocalDateTime data = LocalDateTime.of(2026, 10, 1, 9, 30);
        Favorito novo = favorito().criadoEm(data).build();

        novo.onCreate();

        assertThat(novo.getCriadoEm()).isEqualTo(data);
    }

    @Test
    @DisplayName("alertaHabilitado - sem lista de tipos deve aceitar qualquer alerta")
    void alertaHabilitado_semListaDeveAceitarTodos() {
        Favorito novo = favorito().build();

        assertThat(novo.alertaHabilitado(TipoNotificacao.VARIACAO_ALTA)).isTrue();
        assertThat(novo.alertaHabilitado(TipoNotificacao.TITULO_VENCENDO)).isTrue();
    }

    @Test
    @DisplayName("alertaHabilitado - com lista de tipos deve aceitar só os escolhidos")
    void alertaHabilitado_comListaDeveFiltrar() {
        Favorito novo = favorito().tiposAlertaAtivos(List.of(TipoNotificacao.VARIACAO_BAIXA)).build();

        assertThat(novo.alertaHabilitado(TipoNotificacao.VARIACAO_BAIXA)).isTrue();
        assertThat(novo.alertaHabilitado(TipoNotificacao.VARIACAO_ALTA)).isFalse();
    }

    @Test
    @DisplayName("alertaHabilitado - com alertas desligados não deve aceitar nenhum")
    void alertaHabilitado_alertasDesligadosDeveRecusar() {
        Favorito novo = favorito().alertasAtivos(false).build();

        assertThat(novo.alertaHabilitado(TipoNotificacao.VARIACAO_ALTA)).isFalse();
    }
}
