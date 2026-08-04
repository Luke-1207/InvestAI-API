package com.investai.api.shared.security;

import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioAutenticadoHelperTest {

    private final UsuarioAutenticadoHelper helper =
            new UsuarioAutenticadoHelper();

    private Usuario usuario;

    @BeforeEach
    void setup() {

        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .role(Role.USUARIO)
                .ativo(true)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        usuario.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornarUsuarioLogado() {

        Usuario resultado = helper.getUsuarioLogado();

        assertSame(usuario, resultado);
    }

    @Test
    void deveRetornarIdUsuarioLogado() {

        UUID id = helper.getIdUsuarioLogado();

        assertEquals(usuario.getId(), id);
    }

}