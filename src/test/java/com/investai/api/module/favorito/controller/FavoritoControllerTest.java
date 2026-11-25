package com.investai.api.module.favorito.controller;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.favorito.dto.FavoritoResponseDTO;
import com.investai.api.module.favorito.dto.FavoritoStatusResponseDTO;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.favorito.service.FavoritoService;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FavoritoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FavoritoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoritoService favoritoService;

    @MockitoBean
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(usuarioId);
    }

    private FavoritoResponseDTO favorito() {
        return FavoritoResponseDTO.builder()
                .id(UUID.randomUUID())
                .tipoAtivo(TipoAtivoFavorito.ACAO_FII_ETF)
                .codigo("PETR4")
                .nome("Petrobras PN")
                .alertasAtivos(true)
                .limiarVariacao(new BigDecimal("5.00"))
                .build();
    }

    @Test
    @DisplayName("GET /favoritos - deve listar os favoritos do usuário")
    void listar_deveRetornar200() throws Exception {
        when(favoritoService.listar(usuarioId)).thenReturn(List.of(favorito()));

        mockMvc.perform(get("/v1/favoritos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PETR4"))
                .andExpect(jsonPath("$[0].alertasAtivos").value(true));
    }

    @Test
    @DisplayName("POST /favoritos - deve retornar 201 ao favoritar")
    void adicionar_deveRetornar201() throws Exception {
        when(favoritoService.adicionar(eq(usuarioId), any())).thenReturn(favorito());

        mockMvc.perform(post("/v1/favoritos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoAtivo\":\"ACAO_FII_ETF\",\"codigo\":\"PETR4\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Petrobras PN"));
    }

    @Test
    @DisplayName("POST /favoritos - deve retornar 400 sem o tipo do ativo")
    void adicionar_deveRetornar400SemTipo() throws Exception {
        mockMvc.perform(post("/v1/favoritos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PETR4\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /favoritos - deve retornar 409 quando já favoritado")
    void adicionar_deveRetornar409() throws Exception {
        when(favoritoService.adicionar(any(), any())).thenThrow(new ConflictException("Este ativo já está nos seus favoritos"));

        mockMvc.perform(post("/v1/favoritos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoAtivo\":\"ACAO_FII_ETF\",\"codigo\":\"PETR4\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /favoritos - deve retornar 422 no limite de favoritos")
    void adicionar_deveRetornar422NoLimite() throws Exception {
        when(favoritoService.adicionar(any(), any())).thenThrow(new BusinessException("Você atingiu o limite de 50 favoritos."));

        mockMvc.perform(post("/v1/favoritos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoAtivo\":\"ACAO_FII_ETF\",\"codigo\":\"PETR4\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /favoritos/status - deve informar se está favoritado")
    void status_deveRetornar200() throws Exception {
        when(favoritoService.status(usuarioId, TipoAtivoFavorito.ACAO_FII_ETF, "PETR4", null))
                .thenReturn(FavoritoStatusResponseDTO.builder().favoritado(true).favoritoId(UUID.randomUUID()).build());

        mockMvc.perform(get("/v1/favoritos/status").param("tipoAtivo", "ACAO_FII_ETF").param("codigo", "PETR4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritado").value(true));
    }

    @Test
    @DisplayName("PUT /favoritos/{id}/alertas - deve retornar 400 com limiar acima de 100")
    void configurarAlertas_deveValidarLimiar() throws Exception {
        mockMvc.perform(put("/v1/favoritos/{id}/alertas", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alertasAtivos\":true,\"limiarVariacao\":150}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /favoritos/{id}/alertas - deve retornar 200 com a configuração aplicada")
    void configurarAlertas_deveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(favoritoService.configurarAlertas(eq(usuarioId), eq(id), any())).thenReturn(favorito());

        mockMvc.perform(put("/v1/favoritos/{id}/alertas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alertasAtivos\":true,\"tiposAlertaAtivos\":[\"VARIACAO_ALTA\"],\"limiarVariacao\":3.5}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /favoritos/{id} - deve retornar 204")
    void remover_deveRetornar204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/v1/favoritos/{id}", id))
                .andExpect(status().isNoContent());

        verify(favoritoService).remover(usuarioId, id);
    }
}
