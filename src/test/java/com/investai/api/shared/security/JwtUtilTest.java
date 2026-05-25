package com.investai.api.shared.security;

import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        jwtUtil = new JwtUtil(
                "minha-chave-super-segura-com-pelo-menos-32-caracteres",
                1000 * 60 * 15, // 15 minutos
                7 // dias de expiração do refresh token
        );

        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .role(Role.USUARIO)
                .build();
    }

    @Test
    void deveGerarTokenValido() {
        String token = jwtUtil.gerarToken(usuario);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void deveExtrairEmailDoToken() {
        String token = jwtUtil.gerarToken(usuario);

        String email = jwtUtil.extrairEmail(token);

        assertEquals(usuario.getEmail(), email);
    }

    @Test
    void deveExtrairUserIdDoToken() {
        String token = jwtUtil.gerarToken(usuario);

        String userId = jwtUtil.extrairUserId(token);

        assertEquals(usuario.getId().toString(), userId);
    }

    @Test
    void deveExtrairRoleDoToken() {
        String token = jwtUtil.gerarToken(usuario);

        String role = jwtUtil.extrairRole(token);

        assertEquals(usuario.getRole().name(), role);
    }

    @Test
    void deveRetornarTrueQuandoTokenForValido() {
        String token = jwtUtil.gerarToken(usuario);

        boolean valido = jwtUtil.tokenValido(token);

        assertTrue(valido);
    }

    @Test
    void deveRetornarFalseQuandoTokenForInvalido() {
        String tokenInvalido = "token.invalido.qualquer";

        boolean valido = jwtUtil.tokenValido(tokenInvalido);

        assertFalse(valido);
    }

    @Test
    void deveRetornarFalseQuandoTokenForNulo() {
        boolean valido = jwtUtil.tokenValido(null);

        assertFalse(valido);
    }

    @Test
    void deveGerarTokenComClaimsCorretas() {
        String token = jwtUtil.gerarToken(usuario);

        assertAll(
                () -> assertEquals(usuario.getEmail(), jwtUtil.extrairEmail(token)),
                () -> assertEquals(usuario.getId().toString(), jwtUtil.extrairUserId(token)),
                () -> assertEquals(usuario.getRole().name(), jwtUtil.extrairRole(token))
        );
    }
}