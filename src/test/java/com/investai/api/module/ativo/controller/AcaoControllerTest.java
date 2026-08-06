package com.investai.api.module.ativo.controller;

import com.investai.api.infra.exception.ConflictException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.AcaoResponseDTO;
import com.investai.api.module.ativo.dto.AtualizarAcaoRequestDTO;
import com.investai.api.module.ativo.dto.CadastroAcaoRequestDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.service.AcaoService;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AcaoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AcaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AcaoService acaoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("POST /acoes - deve cadastrar ativo com sucesso")
    void cadastrar_deveRetornar201ComSucesso() throws Exception {
        CadastroAcaoRequestDTO request = new CadastroAcaoRequestDTO();
        request.setCodigo("TAEE3");
        request.setNome("Taesa - Transmissão de Energia");
        request.setTipo(TipoAtivo.ACAO);
        request.setSetor("Energia Elétrica");

        AcaoResponseDTO response = criarResponseMock();

        when(acaoService.cadastrar(any(CadastroAcaoRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/v1/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("TAEE3"))
                .andExpect(jsonPath("$.tipo").value("ACAO"));
    }

    @Test
    @DisplayName("POST /acoes - deve retornar 409 quando código já cadastrado")
    void cadastrar_deveRetornar409QuandoCodigoDuplicado() throws Exception {
        CadastroAcaoRequestDTO request = new CadastroAcaoRequestDTO();
        request.setCodigo("TAEE3");
        request.setNome("Taesa");
        request.setTipo(TipoAtivo.ACAO);

        when(acaoService.cadastrar(any(CadastroAcaoRequestDTO.class)))
                .thenThrow(new ConflictException("Já existe um ativo cadastrado com esse código"));

        mockMvc.perform(post("/v1/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Já existe um ativo cadastrado com esse código"));
    }

    @Test
    @DisplayName("POST /acoes - deve retornar 400 quando nome vazio")
    void cadastrar_deveRetornar400QuandoNomeVazio() throws Exception {
        CadastroAcaoRequestDTO request = new CadastroAcaoRequestDTO();
        request.setCodigo("TAEE3");
        request.setNome("");
        request.setTipo(TipoAtivo.ACAO);

        mockMvc.perform(post("/v1/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de validação"));
    }

    @Test
    @DisplayName("POST /acoes - deve retornar 400 quando tipo é um valor inválido no JSON")
    void cadastrar_deveRetornar400QuandoTipoInvalidoNoJson() throws Exception {
        String jsonComTipoInvalido = """
            { "codigo": "PETR4", "nome": "Petrobras", "tipo": "CRYPTO", "setor": "Energia" }
        """;

        mockMvc.perform(post("/v1/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonComTipoInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /acoes/{id} - deve atualizar ativo com sucesso")
    void atualizar_deveRetornar200ComSucesso() throws Exception {
        UUID id = UUID.randomUUID();

        AtualizarAcaoRequestDTO request = new AtualizarAcaoRequestDTO();
        request.setNome("Taesa S.A.");
        request.setTipo(TipoAtivo.ACAO);
        request.setSetor("Energia");
        request.setAtivo(true);

        AcaoResponseDTO response = criarResponseMock();

        when(acaoService.atualizar(eq(id), any(AtualizarAcaoRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/v1/acoes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("TAEE3"));
    }

    @Test
    @DisplayName("PUT /acoes/{id} - deve retornar 404 quando ativo não encontrado")
    void atualizar_deveRetornar404QuandoNaoEncontrado() throws Exception {
        UUID id = UUID.randomUUID();

        AtualizarAcaoRequestDTO request = new AtualizarAcaoRequestDTO();
        request.setNome("Qualquer");
        request.setTipo(TipoAtivo.ACAO);
        request.setAtivo(true);

        when(acaoService.atualizar(eq(id), any(AtualizarAcaoRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Ativo não encontrado"));

        mockMvc.perform(put("/v1/acoes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /acoes/{id} - deve retornar 400 quando ativo (status) não informado")
    void atualizar_deveRetornar400QuandoAtivoNulo() throws Exception {
        UUID id = UUID.randomUUID();

        String jsonSemAtivo = """
            { "nome": "Taesa", "tipo": "ACAO", "setor": "Energia" }
        """;

        mockMvc.perform(put("/v1/acoes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSemAtivo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /acoes/{id} - deve desativar ativo com sucesso")
    void desativar_deveRetornar204ComSucesso() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/v1/acoes/{id}", id))
                .andExpect(status().isNoContent());

        verify(acaoService).desativar(id);
    }

    @Test
    @DisplayName("DELETE /acoes/{id} - deve retornar 404 quando ativo não encontrado")
    void desativar_deveRetornar404QuandoNaoEncontrado() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Ativo não encontrado"))
                .when(acaoService).desativar(id);

        mockMvc.perform(delete("/v1/acoes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /acoes/{id} - deve retornar ativo com sucesso")
    void buscarPorId_deveRetornar200ComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        AcaoResponseDTO response = criarResponseMock();

        when(acaoService.buscarPorId(id)).thenReturn(response);

        mockMvc.perform(get("/v1/acoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("TAEE3"))
                .andExpect(jsonPath("$.nome").value(response.getNome()));
    }

    @Test
    @DisplayName("GET /acoes/{id} - deve retornar 404 quando ativo não encontrado")
    void buscarPorId_deveRetornar404QuandoNaoEncontrado() throws Exception {
        UUID id = UUID.randomUUID();

        when(acaoService.buscarPorId(id))
                .thenThrow(new ResourceNotFoundException("Ativo não encontrado"));

        mockMvc.perform(get("/v1/acoes/{id}", id))
                .andExpect(status().isNotFound());
    }

    private AcaoResponseDTO criarResponseMock() {
        return AcaoResponseDTO.builder()
                .id(UUID.randomUUID())
                .codigo("TAEE3")
                .nome("Taesa - Transmissão de Energia")
                .tipo(TipoAtivo.ACAO)
                .setor("Energia Elétrica")
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }
}