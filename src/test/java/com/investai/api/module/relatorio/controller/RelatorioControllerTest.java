package com.investai.api.module.relatorio.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.relatorio.dto.RelatorioGeradoDTO;
import com.investai.api.module.relatorio.service.RelatorioService;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RelatorioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RelatorioControllerTest {

    private static final byte[] PDF = {37, 80, 68, 70};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioService relatorioService;

    @MockitoBean
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(UUID.randomUUID()).nome("Lucas").email("lucas@email.com").role(Role.USUARIO).ativo(true).build();
        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuario);
    }

    @Test
    @DisplayName("GET /relatorios/ativo/{identificador}?tipo=VARIAVEL - deve retornar o PDF com headers de download")
    void gerarRelatorioAtivo_variavel_deveRetornarPdfComHeadersDeDownload() throws Exception {
        when(relatorioService.gerarRelatorioAtivoVariavel("TAEE11", usuario))
                .thenReturn(new RelatorioGeradoDTO("analise-TAEE11.pdf", PDF));

        mockMvc.perform(get("/v1/relatorios/ativo/TAEE11").param("tipo", "VARIAVEL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analise-TAEE11.pdf\""))
                .andExpect(content().bytes(PDF));
    }

    @Test
    @DisplayName("GET /relatorios/ativo/{identificador} - sem tipo deve assumir renda variável")
    void gerarRelatorioAtivo_semTipo_deveAssumirVariavel() throws Exception {
        when(relatorioService.gerarRelatorioAtivoVariavel("TAEE11", usuario))
                .thenReturn(new RelatorioGeradoDTO("analise-TAEE11.pdf", PDF));

        mockMvc.perform(get("/v1/relatorios/ativo/TAEE11"))
                .andExpect(status().isOk());

        verify(relatorioService, never()).gerarRelatorioAtivoFixo(any(), any());
    }

    @Test
    @DisplayName("GET /relatorios/ativo/{identificador}?tipo=FIXA - deve gerar o relatório de renda fixa")
    void gerarRelatorioAtivo_fixa_deveGerarRelatorioDeRendaFixa() throws Exception {
        when(relatorioService.gerarRelatorioAtivoFixo("SELIC2029", usuario))
                .thenReturn(new RelatorioGeradoDTO("analise-SELIC2029.pdf", PDF));

        mockMvc.perform(get("/v1/relatorios/ativo/SELIC2029").param("tipo", "FIXA"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analise-SELIC2029.pdf\""));

        verify(relatorioService, never()).gerarRelatorioAtivoVariavel(any(), any());
    }

    @Test
    @DisplayName("GET /relatorios/ativo/{identificador} - tipo inválido deve retornar 400")
    void gerarRelatorioAtivo_tipoInvalido_deveRetornar400() throws Exception {
        mockMvc.perform(get("/v1/relatorios/ativo/TAEE11").param("tipo", "CRIPTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido para o parâmetro: tipo"));

        verifyNoInteractions(relatorioService);
    }

    @Test
    @DisplayName("GET /relatorios/ativo/{identificador} - ativo inexistente deve retornar 404")
    void gerarRelatorioAtivo_ativoInexistente_deveRetornar404() throws Exception {
        when(relatorioService.gerarRelatorioAtivoVariavel("XXXX", usuario))
                .thenThrow(new ResourceNotFoundException("Ativo não cadastrado ou inativo: XXXX"));

        mockMvc.perform(get("/v1/relatorios/ativo/XXXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Ativo não cadastrado ou inativo: XXXX"));
    }
}
