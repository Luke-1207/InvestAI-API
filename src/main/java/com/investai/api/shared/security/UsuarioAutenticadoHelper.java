package com.investai.api.shared.security;

import com.investai.api.module.auth.entity.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioAutenticadoHelper {
    public Usuario getUsuarioLogado() {
        return (Usuario) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    public java.util.UUID getIdUsuarioLogado() {
        return getUsuarioLogado().getId();
    }
}
