package com.investai.api.module.auth.service;

import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void deveRetornarUsuarioQuandoEmailExistir() {

        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .senha("123456")
                .role(Role.USUARIO)
                .ativo(true)
                .build();

        when(usuarioRepository.findByEmail(usuario.getEmail()))
                .thenReturn(Optional.of(usuario));

        UserDetails resultado =
                usuarioDetailsService.loadUserByUsername(usuario.getEmail());

        assertNotNull(resultado);

        assertEquals(usuario.getEmail(), resultado.getUsername());

        verify(usuarioRepository)
                .findByEmail(usuario.getEmail());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExistir() {

        String email = "naoexiste@email.com";

        when(usuarioRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> usuarioDetailsService.loadUserByUsername(email)
        );

        assertEquals(
                "Usuário não encontrado",
                exception.getMessage()
        );

        verify(usuarioRepository)
                .findByEmail(email);
    }
}