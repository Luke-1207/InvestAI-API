package com.investai.api.module.perfil.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.perfil.dto.QuizResponseDTO;
import com.investai.api.module.perfil.service.PerfilQuizService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PerfilController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class PerfilControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilQuizService perfilQuizService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private String tokenUsuario;
    private String tokenGestor;

    @BeforeEach
    void setUp() {
        tokenUsuario = "token-usuario-mock";
        tokenGestor = "token-gestor-mock";

        Usuario usuarioComum = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Silva")
                .email("lucas@email.com")
                .senha("senha-encoded")
                .role(Role.USUARIO)
                .ativo(true)
                .build();

        Usuario gestorMock = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Admin Gestor")
                .email("gestor@email.com")
                .senha("senha-encoded")
                .role(Role.GESTOR)
                .ativo(true)
                .build();

        when(jwtUtil.tokenValido(tokenUsuario)).thenReturn(true);
        when(jwtUtil.tokenValido(tokenGestor)).thenReturn(true);

        when(jwtUtil.extrairEmail(tokenUsuario)).thenReturn(usuarioComum.getEmail());
        when(jwtUtil.extrairEmail(tokenGestor)).thenReturn(gestorMock.getEmail());

        when(usuarioDetailsService.loadUserByUsername(usuarioComum.getEmail()))
                .thenReturn(usuarioComum);
        when(usuarioDetailsService.loadUserByUsername(gestorMock.getEmail()))
                .thenReturn(gestorMock);
    }

    @Test
    @DisplayName("GET /perfil/quiz - deve retornar 401 sem token")
    void obterQuiz_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/perfil/quiz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /perfil/quiz - deve permitir usuário comum autenticado")
    void obterQuiz_devePermitirUsuarioComum() throws Exception {
        when(perfilQuizService.obterQuiz())
                .thenReturn(QuizResponseDTO.builder().perguntas(List.of()).build());

        mockMvc.perform(get("/v1/perfil/quiz")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /perfil/quiz - deve permitir gestor autenticado")
    void obterQuiz_devePermitirGestor() throws Exception {
        when(perfilQuizService.obterQuiz())
                .thenReturn(QuizResponseDTO.builder().perguntas(List.of()).build());

        mockMvc.perform(get("/v1/perfil/quiz")
                        .header("Authorization", "Bearer " + tokenGestor))
                .andExpect(status().isOk());
    }
}