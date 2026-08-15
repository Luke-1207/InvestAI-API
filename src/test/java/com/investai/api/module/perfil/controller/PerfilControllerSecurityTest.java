package com.investai.api.module.perfil.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.dto.QuizResponseDTO;
import com.investai.api.module.perfil.dto.QuizSubmissaoResponseDTO;
import com.investai.api.module.perfil.service.PerfilQuizService;
import com.investai.api.module.perfil.service.PerfilService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @MockitoBean
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @MockitoBean
    private PerfilService perfilService;

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
    @DisplayName("GET /perfil - deve retornar 401 sem token")
    void obterPerfil_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /perfil - deve permitir usuário comum autenticado")
    void obterPerfil_devePermitirUsuarioComum() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());
        when(perfilService.obterPerfil(any())).thenReturn(PerfilResponseDTO.builder().build());

        mockMvc.perform(get("/v1/perfil")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /perfil - deve retornar 401 sem token")
    void editarPerfil_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(put("/v1/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
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

    @Test
    @DisplayName("PUT /perfil/quiz - deve retornar 401 sem token")
    void submeterQuiz_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(put("/v1/perfil/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"respostas\": [] }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /perfil/quiz - deve permitir usuário comum autenticado")
    void submeterQuiz_devePermitirUsuarioComum() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());
        when(perfilQuizService.submeterQuiz(any(), any()))
                .thenReturn(QuizSubmissaoResponseDTO.builder().build());

        String body = """
                {
                  "respostas": [
                    { "perguntaId": "%s", "opcaoIds": ["%s"] }
                  ]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(put("/v1/perfil/quiz")
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}