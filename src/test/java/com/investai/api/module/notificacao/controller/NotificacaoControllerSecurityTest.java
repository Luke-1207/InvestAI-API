package com.investai.api.module.notificacao.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.notificacao.dto.ComunicadoSistemaResponseDTO;
import com.investai.api.module.notificacao.dto.ContagemNaoLidasResponseDTO;
import com.investai.api.module.notificacao.service.NotificacaoService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificacaoController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class NotificacaoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacaoService notificacaoService;

    @MockitoBean
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private static final String TOKEN_USUARIO = "token-usuario-mock";
    private static final String TOKEN_GESTOR = "token-gestor-mock";
    private static final String COMUNICADO = "{\"titulo\":\"Manutenção\",\"mensagem\":\"Sistema fora do ar às 22h\"}";

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().id(UUID.randomUUID()).nome("Usuário").email("usuario@email.com")
                .senha("x").role(Role.USUARIO).ativo(true).build();
        Usuario gestor = Usuario.builder().id(UUID.randomUUID()).nome("Gestor").email("gestor@email.com")
                .senha("x").role(Role.GESTOR).ativo(true).build();
        when(jwtUtil.tokenValido(TOKEN_USUARIO)).thenReturn(true);
        when(jwtUtil.tokenValido(TOKEN_GESTOR)).thenReturn(true);
        when(jwtUtil.extrairEmail(TOKEN_USUARIO)).thenReturn(usuario.getEmail());
        when(jwtUtil.extrairEmail(TOKEN_GESTOR)).thenReturn(gestor.getEmail());
        when(usuarioDetailsService.loadUserByUsername(usuario.getEmail())).thenReturn(usuario);
        when(usuarioDetailsService.loadUserByUsername(gestor.getEmail())).thenReturn(gestor);
    }

    @Test
    @DisplayName("GET /notificacoes - deve retornar 401 sem token")
    void listar_deveRetornar401() throws Exception {
        mockMvc.perform(get("/v1/notificacoes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /notificacoes/nao-lidas/contagem - deve permitir usuário comum")
    void contagem_devePermitirUsuario() throws Exception {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(UUID.randomUUID());
        when(notificacaoService.contarNaoLidas(any())).thenReturn(new ContagemNaoLidasResponseDTO(0));

        mockMvc.perform(get("/v1/notificacoes/nao-lidas/contagem").header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /notificacoes/sistema - deve retornar 401 sem token")
    void comunicado_deveRetornar401() throws Exception {
        mockMvc.perform(post("/v1/notificacoes/sistema").contentType(MediaType.APPLICATION_JSON).content(COMUNICADO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /notificacoes/sistema - deve retornar 403 para usuário comum")
    void comunicado_deveRetornar403ParaUsuario() throws Exception {
        mockMvc.perform(post("/v1/notificacoes/sistema")
                        .header("Authorization", "Bearer " + TOKEN_USUARIO)
                        .contentType(MediaType.APPLICATION_JSON).content(COMUNICADO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /notificacoes/sistema - deve permitir gestor")
    void comunicado_devePermitirGestor() throws Exception {
        when(notificacaoService.enviarComunicado(any())).thenReturn(new ComunicadoSistemaResponseDTO(3));

        mockMvc.perform(post("/v1/notificacoes/sistema")
                        .header("Authorization", "Bearer " + TOKEN_GESTOR)
                        .contentType(MediaType.APPLICATION_JSON).content(COMUNICADO))
                .andExpect(status().isCreated());
    }
}
