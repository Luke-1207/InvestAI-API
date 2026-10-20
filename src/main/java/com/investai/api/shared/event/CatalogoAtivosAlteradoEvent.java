package com.investai.api.shared.event;

import org.springframework.context.ApplicationEvent;

public class CatalogoAtivosAlteradoEvent extends ApplicationEvent {

    public CatalogoAtivosAlteradoEvent(Object source) {
        super(source);
    }
}
