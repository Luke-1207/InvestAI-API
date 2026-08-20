package com.investai.api.module.rendafixa.controller;

import com.investai.api.config.SecurityConfig;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.rendafixa.dto.TituloPrivadoResponseDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.service.TituloPrivadoDetalheService;
import com.investai.api.module.rendafixa.service.TituloPrivadoListagemService;
import com.investai.api.module.rendafixa.service.TituloPrivadoService;
import com.investai.api.shared.security.JwtAuthFilter;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TituloPrivadoController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class TituloPrivadoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TituloPrivadoService tituloPrivadoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @MockitoBean
    private TituloPrivadoListagemService tituloPrivadoListagemService;

    @MockitoBean
    private TituloPrivadoDetalheService tituloPrivadoDetalheService;

    private String tokenUsuario;
    private String tokenGestor;

    private final String CADASTRO_BODY = """
            {
              "tipo": "CDB",
              "emissor": "Banco Inter",
              "indexador": "CDI",
              "taxaPercentual": 112.0,
              "vencimento": "%s",
              "investimentoMinimo": 500.00,
              "liquidez": "DIARIA",
              "garantidoFgc": true,
              "isentoIr": false
            }
            """.formatted(LocalDate.now().plusYears(2));

    @BeforeEach
    void setUp() {
        tokenUsuario = "token-usuario-mock";
        tokenGestor = "token-gestor-mock";

        Usuario usuarioComum = Usuario.builder()
                .id(UUID.randomUUID()).nome("Lucas").email("lucas@email.com")
                .senha("senha-encoded").role(Role.USUARIO).ativo(true).build();

        Usuario gestor = Usuario.builder()
                .id(UUID.randomUUID()).nome("Gestor").email("gestor@email.com")
                .senha("senha-encoded").role(Role.GESTOR).ativo(true).build();

        when(jwtUtil.tokenValido(tokenUsuario)).thenReturn(true);
        when(jwtUtil.tokenValido(tokenGestor)).thenReturn(true);
        when(jwtUtil.extrairEmail(tokenUsuario)).thenReturn(usuarioComum.getEmail());
        when(jwtUtil.extrairEmail(tokenGestor)).thenReturn(gestor.getEmail());
        when(usuarioDetailsService.loadUserByUsername(usuarioComum.getEmail())).thenReturn(usuarioComum);
        when(usuarioDetailsService.loadUserByUsername(gestor.getEmail())).thenReturn(gestor);
    }

    @Test
    @DisplayName("POST /renda-fixa/titulos - deve retornar 403 pra usuário comum")
    void cadastrar_deveRetornar403ParaUsuarioComum() throws Exception {
        mockMvc.perform(post("/v1/renda-fixa/titulos")
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRO_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /renda-fixa/titulos - deve permitir gestor")
    void cadastrar_devePermitirGestor() throws Exception {
        when(tituloPrivadoService.cadastrar(any())).thenReturn(TituloPrivadoResponseDTO.builder()
                .id(UUID.randomUUID()).tipo(TipoTituloPrivado.CDB).emissor("Banco Inter")
                .indexador(Indexador.CDI).taxaPercentual(BigDecimal.valueOf(112.0))
                .vencimento(LocalDate.now().plusYears(2)).investimentoMinimo(BigDecimal.valueOf(500.00))
                .liquidez(TipoLiquidez.DIARIA).garantidoFgc(true).isentoIr(false).ativo(true).build());

        mockMvc.perform(post("/v1/renda-fixa/titulos")
                        .header("Authorization", "Bearer " + tokenGestor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRO_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PATCH /renda-fixa/titulos/{id}/status - deve retornar 403 pra usuário comum")
    void alterarStatus_deveRetornar403ParaUsuarioComum() throws Exception {
        mockMvc.perform(patch("/v1/renda-fixa/titulos/{id}/status", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ativo\": false }"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /renda-fixa/titulos/{id} - deve retornar 403 pra usuário comum")
    void desativar_deveRetornar403ParaUsuarioComum() throws Exception {
        mockMvc.perform(delete("/v1/renda-fixa/titulos/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /renda-fixa/titulos - deve retornar 401 sem token")
    void cadastrar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/v1/renda-fixa/titulos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRO_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /renda-fixa/titulos - deve retornar 401 sem token")
    void listar_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/renda-fixa/titulos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /renda-fixa/titulos - deve permitir usuário comum autenticado")
    void listar_devePermitirUsuarioComum() throws Exception {
        when(tituloPrivadoListagemService.listar(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));

        mockMvc.perform(get("/v1/renda-fixa/titulos")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }
}