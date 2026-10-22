package com.investai.api.shared.event;

import org.springframework.context.ApplicationEvent;

public class IndicadoresMercadoAlteradosEvent extends ApplicationEvent {

    public IndicadoresMercadoAlteradosEvent(Object source) {
        super(source);
    }
}
