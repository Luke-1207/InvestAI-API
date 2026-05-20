package com.investai.api.shared.security;

import com.investai.api.module.auth.service.UsuarioDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UsuarioDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveContinuarFiltroQuandoAuthorizationForNulo()
            throws ServletException, IOException {

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void deveContinuarFiltroQuandoAuthorizationNaoComecarComBearer()
            throws ServletException, IOException {

        request.addHeader("Authorization", "Basic abc123");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void deveContinuarFiltroQuandoTokenForInvalido()
            throws ServletException, IOException {

        request.addHeader("Authorization", "Bearer token-invalido");

        when(jwtUtil.tokenValido("token-invalido")).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        verify(jwtUtil).tokenValido("token-invalido");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void deveAutenticarUsuarioQuandoTokenForValido()
            throws ServletException, IOException {

        String token = "token-valido";
        String email = "lucas@email.com";

        request.addHeader("Authorization", "Bearer " + token);

        UserDetails userDetails = new User(
                email,
                "123",
                Collections.emptyList()
        );

        when(jwtUtil.tokenValido(token)).thenReturn(true);
        when(jwtUtil.extrairEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email))
                .thenReturn(userDetails);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil).tokenValido(token);
        verify(jwtUtil).extrairEmail(token);
        verify(userDetailsService).loadUserByUsername(email);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        assertEquals(
                email,
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getName()
        );

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void naoDeveAutenticarQuandoJaExistirAuthenticationNoContext()
            throws ServletException, IOException {

        String token = "token-valido";
        String email = "lucas@email.com";

        request.addHeader("Authorization", "Bearer " + token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "usuario-existente",
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(jwtUtil.tokenValido(token)).thenReturn(true);
        when(jwtUtil.extrairEmail(token)).thenReturn(email);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, never())
                .loadUserByUsername(anyString());

        assertEquals(
                "usuario-existente",
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()
        );

        verify(filterChain).doFilter(request, response);
    }
}