package com.investai.api.module.notificacao.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificacaoTest {

    private Notificacao.NotificacaoBuilder notificacao() {
        return Notificacao.builder().usuarioId(UUID.randomUUID()).tipo(TipoNotificacao.SISTEMA)
                .titulo("Novos títulos").mensagem("Novos títulos adicionados à plataforma.");
    }

    @Test
    @DisplayName("builder - deve nascer como não lida e sem ativo")
    void builder_deveNascerNaoLida() {
        Notificacao nova = notificacao().build();

        assertThat(nova.isLida()).isFalse();
        assertThat(nova.getLidaEm()).isNull();
        assertThat(nova.getAtivoId()).isNull();
        assertThat(nova.getTipoAtivo()).isNull();
    }

    @Test
    @DisplayName("onCreate - deve registrar a data de criação ao persistir")
    void onCreate_devePreencherCriadoEm() {
        Notificacao nova = notificacao().build();

        nova.onCreate();

        assertThat(nova.getCriadoEm()).isNotNull();
    }

    @Test
    @DisplayName("marcarComoLida - deve marcar e guardar quando foi lida")
    void marcarComoLida_deveRegistrarALeitura() {
        Notificacao nova = notificacao().build();
        LocalDateTime agora = LocalDateTime.of(2026, 10, 26, 20, 0);

        nova.marcarComoLida(agora);

        assertThat(nova.isLida()).isTrue();
        assertThat(nova.getLidaEm()).isEqualTo(agora);
    }

    @Test
    @DisplayName("marcarComoLida - marcar de novo não deve mudar a data da primeira leitura")
    void marcarComoLida_deveSerIdempotente() {
        Notificacao nova = notificacao().build();
        LocalDateTime primeira = LocalDateTime.of(2026, 10, 26, 20, 0);
        nova.marcarComoLida(primeira);

        nova.marcarComoLida(primeira.plusHours(3));

        assertThat(nova.getLidaEm()).isEqualTo(primeira);
    }
}
