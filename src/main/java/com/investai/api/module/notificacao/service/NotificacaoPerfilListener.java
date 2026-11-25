package com.investai.api.module.notificacao.service;

import com.investai.api.module.notificacao.entity.TipoNotificacao;
import com.investai.api.shared.event.PerfilAlteradoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoPerfilListener {

    static final String TITULO = "Seu perfil de investidor foi atualizado";
    static final String MENSAGEM = "As sugestões do dashboard, a listagem inteligente e os resumos da IA já consideram o seu novo perfil.";

    private final NotificacaoService notificacaoService;

    @TransactionalEventListener(fallbackExecution = true)
    public void aoAlterarPerfil(PerfilAlteradoEvent event) {
        try {
            notificacaoService.criar(event.getUsuarioId(), TipoNotificacao.PERFIL_ALTERADO, TITULO, MENSAGEM, null, null);
        } catch (Exception e) {
            log.warn("Não foi possível registrar a notificação de perfil alterado para {}: {}", event.getUsuarioId(), e.getMessage());
        }
    }
}
