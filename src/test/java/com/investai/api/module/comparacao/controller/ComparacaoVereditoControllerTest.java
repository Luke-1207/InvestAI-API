package com.investai.api.module.comparacao.controller;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.infra.exception.IaIndisponivelException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.comparacao.dto.TipoAtivoComparacao;
import com.investai.api.module.comparacao.dto.VereditoComparacaoResponseDTO;
import com.investai.api.module.comparacao.service.ComparacaoVereditoService;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ComparacaoVereditoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ComparacaoVereditoControllerTest {

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

    private final UUID usuarioId = UUID.randomUUID();
    private final String tituloId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(usuarioId);
    }

    private MockHttpServletRequestBuilder requisicao() {
        return get("/v1/comparacao/veredito")
                .param("tipoA", "ACAO").param("identificadorA", "TAEE11")
                .param("tipoB", "CDB").param("identificadorB", tituloId);
    }

    private void servicoLanca(RuntimeException excecao) {
        when(comparacaoVereditoService.obterVeredito(any(), any(), any(), any(), any())).thenThrow(excecao);
    }

    @Test
    @DisplayName("GET /comparacao/veredito - deve retornar o veredito para o usuário logado")
    void obterVeredito_deveRetornarVeredito() throws Exception {
        when(comparacaoVereditoService.obterVeredito(
                usuarioId, TipoAtivoComparacao.ACAO, "TAEE11", TipoAtivoComparacao.CDB, tituloId))
                .thenReturn(new VereditoComparacaoResponseDTO("TAEE11 combina mais com você.", false));

        mockMvc.perform(requisicao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veredito").value("TAEE11 combina mais com você."))
                .andExpect(jsonPath("$.simplificado").value(false));

        verify(comparacaoVereditoService).obterVeredito(
                eq(usuarioId), eq(TipoAtivoComparacao.ACAO), eq("TAEE11"), eq(TipoAtivoComparacao.CDB), eq(tituloId));
    }

    @Test
    @DisplayName("GET /comparacao/veredito - deve retornar 502 tratado quando a IA não responde")
    void obterVeredito_deveRetornar502QuandoIaIndisponivel() throws Exception {
        servicoLanca(new IaIndisponivelException("Serviço de comparação por IA não respondeu a tempo"));

        mockMvc.perform(requisicao())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    @DisplayName("GET /comparacao/veredito - deve retornar 422 quando o perfil não está preenchido")
    void obterVeredito_deveRetornar422QuandoRegraDeNegocioFalha() throws Exception {
        servicoLanca(new BusinessException("Preencha seu perfil de investidor para receber o veredito da comparação"));

        mockMvc.perform(requisicao())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    @DisplayName("GET /comparacao/veredito - deve retornar 404 quando um dos ativos não existe")
    void obterVeredito_deveRetornar404QuandoAtivoNaoExiste() throws Exception {
        servicoLanca(new ResourceNotFoundException("Título privado não encontrado"));

        mockMvc.perform(requisicao()).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /comparacao/veredito - deve retornar 400 para tipo de ativo desconhecido")
    void obterVeredito_deveRetornar400ParaTipoInvalido() throws Exception {
        mockMvc.perform(get("/v1/comparacao/veredito")
                        .param("tipoA", "CRIPTO").param("identificadorA", "BTC")
                        .param("tipoB", "ACAO").param("identificadorB", "TAEE11"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(comparacaoVereditoService);
    }

    @Test
    @DisplayName("GET /comparacao/veredito - deve retornar 400 quando falta um parâmetro")
    void obterVeredito_deveRetornar400QuandoFaltaParametro() throws Exception {
        mockMvc.perform(get("/v1/comparacao/veredito")
                        .param("tipoA", "ACAO").param("identificadorA", "TAEE11")
                        .param("tipoB", "FII"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(comparacaoVereditoService);
    }
}
