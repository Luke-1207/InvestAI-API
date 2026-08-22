package com.investai.api.shared.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PerfilAlteradoEvent extends ApplicationEvent {

    private final UUID usuarioId;

    public PerfilAlteradoEvent(Object source, UUID usuarioId) {
        super(source);
        this.usuarioId = usuarioId;
    }
}