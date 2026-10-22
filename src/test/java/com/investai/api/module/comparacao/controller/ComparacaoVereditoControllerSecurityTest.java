package com.investai.api.module.comparacao.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.comparacao.dto.VereditoComparacaoResponseDTO;
import com.investai.api.module.comparacao.service.ComparacaoVereditoService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ComparacaoVereditoController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class ComparacaoVereditoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComparacaoVereditoService comparacaoVereditoService;

    @MockitoBean
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private final String tokenUsuario = "token-usuario-mock";

    @BeforeEach
    void setUp() {
        Usuario usuarioComum = Usuario.builder()
                .id(UUID.randomUUID()).nome("Lucas").email("lucas@email.com")
                .senha("senha-encoded").role(Role.USUARIO).ativo(true).build();

        when(jwtUtil.tokenValido(tokenUsuario)).thenReturn(true);
        when(jwtUtil.extrairEmail(tokenUsuario)).thenReturn(usuarioComum.getEmail());
        when(usuarioDetailsService.loadUserByUsername(usuarioComum.getEmail())).thenReturn(usuarioComum);
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(usuarioComum.getId());
    }

    private MockHttpServletRequestBuilder requisicao() {
        return get("/v1/comparacao/veredito")
                .param("tipoA", "ACAO").param("identificadorA", "TAEE11")
                .param("tipoB", "FII").param("identificadorB", "HGLG11");
    }

    @Test
    @DisplayName("GET /comparacao/veredito - deve retornar 401 sem token")
    void obterVeredito_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(requisicao()).andExpect(status().isUnauthorized());

        verifyNoInteractions(comparacaoVereditoService);
    }

    @Test
    @DisplayName("GET /comparacao/veredito - deve permitir usuário comum autenticado")
    void obterVeredito_devePermitirUsuarioComum() throws Exception {
        when(comparacaoVereditoService.obterVeredito(any(), any(), any(), any(), any()))
                .thenReturn(new VereditoComparacaoResponseDTO("Veredito", false));

        mockMvc.perform(requisicao().header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }
}
