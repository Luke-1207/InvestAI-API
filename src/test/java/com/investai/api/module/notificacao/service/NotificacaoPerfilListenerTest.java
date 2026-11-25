package com.investai.api.module.notificacao.service;

import com.investai.api.module.notificacao.entity.TipoNotificacao;
import com.investai.api.shared.event.PerfilAlteradoEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoPerfilListenerTest {

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private NotificacaoPerfilListener listener;

    @Test
    @DisplayName("deve criar notificação PERFIL_ALTERADO para o usuário do evento")
    void deveCriarNotificacao() {
        UUID usuarioId = UUID.randomUUID();

        listener.aoAlterarPerfil(new PerfilAlteradoEvent(this, usuarioId));

        verify(notificacaoService).criar(usuarioId, TipoNotificacao.PERFIL_ALTERADO,
                NotificacaoPerfilListener.TITULO, NotificacaoPerfilListener.MENSAGEM, null, null);
    }

    @Test
    @DisplayName("falha ao notificar não deve propagar para quem alterou o perfil")
    void falhaNaoDevePropagar() {
        when(notificacaoService.criar(any(), any(), any(), any(), any(), any())).thenThrow(new RuntimeException("banco fora"));

        assertThatCode(() -> listener.aoAlterarPerfil(new PerfilAlteradoEvent(this, UUID.randomUUID())))
                .doesNotThrowAnyException();
    }
}
