package com.investai.api.module.relatorio.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.relatorio.dto.RelatorioGeradoDTO;
import com.investai.api.module.relatorio.service.HistoricoRelatorioService;
import com.investai.api.module.relatorio.service.RelatorioService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RelatorioController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class RelatorioControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioService relatorioService;

    @MockitoBean
    private HistoricoRelatorioService historicoRelatorioService;

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
        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuarioComum);
    }

    @Test
    @DisplayName("GET /relatorios/ativo/{identificador} - deve retornar 401 sem token")
    void gerarRelatorioAtivo_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/relatorios/ativo/TAEE11"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(relatorioService);
    }

    @Test
    @DisplayName("GET /relatorios/ativo/{identificador} - deve permitir usuário comum autenticado")
    void gerarRelatorioAtivo_devePermitirUsuarioComum() throws Exception {
        when(relatorioService.gerarRelatorioAtivoVariavel(eq("TAEE11"), any()))
                .thenReturn(new RelatorioGeradoDTO("analise-TAEE11.pdf", new byte[]{37, 80, 68, 70}));

        mockMvc.perform(get("/v1/relatorios/ativo/TAEE11")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /relatorios/listagem - deve retornar 401 sem token")
    void gerarRelatorioListagem_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/v1/relatorios/listagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modulo\": \"VARIAVEL\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(relatorioService);
    }

    @Test
    @DisplayName("POST /relatorios/listagem - deve permitir usuário comum autenticado")
    void gerarRelatorioListagem_devePermitirUsuarioComum() throws Exception {
        when(relatorioService.gerarRelatorioListagem(any(), any()))
                .thenReturn(new RelatorioGeradoDTO("listagem-renda-variavel.pdf", new byte[]{37, 80, 68, 70}));

        mockMvc.perform(post("/v1/relatorios/listagem")
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modulo\": \"VARIAVEL\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /relatorios/perfil - deve retornar 401 sem token")
    void gerarRelatorioPerfil_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/relatorios/perfil"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(relatorioService);
    }

    @Test
    @DisplayName("GET /relatorios/perfil - deve permitir usuário comum autenticado")
    void gerarRelatorioPerfil_devePermitirUsuarioComum() throws Exception {
        when(relatorioService.gerarRelatorioPerfil(any()))
                .thenReturn(new RelatorioGeradoDTO("perfil-investidor.pdf", new byte[]{37, 80, 68, 70}));

        mockMvc.perform(get("/v1/relatorios/perfil")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /relatorios/historico - deve retornar 401 sem token")
    void listarHistorico_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/relatorios/historico"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(historicoRelatorioService);
    }

    @Test
    @DisplayName("GET /relatorios/historico - deve permitir usuário comum autenticado")
    void listarHistorico_devePermitirUsuarioComum() throws Exception {
        when(historicoRelatorioService.listar(any(), anyInt(), anyInt())).thenReturn(Page.empty());

        mockMvc.perform(get("/v1/relatorios/historico")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /relatorios/ativo/{identificador} - CORS deve expor o header Content-Disposition")
    void gerarRelatorioAtivo_corsDeveExporContentDisposition() throws Exception {
        when(relatorioService.gerarRelatorioAtivoVariavel(eq("TAEE11"), any()))
                .thenReturn(new RelatorioGeradoDTO("analise-TAEE11.pdf", new byte[]{37, 80, 68, 70}));

        mockMvc.perform(get("/v1/relatorios/ativo/TAEE11")
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION));
    }
}
