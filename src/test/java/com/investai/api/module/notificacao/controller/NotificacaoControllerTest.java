package com.investai.api.module.notificacao.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.notificacao.dto.ComunicadoSistemaResponseDTO;
import com.investai.api.module.notificacao.dto.ContagemNaoLidasResponseDTO;
import com.investai.api.module.notificacao.dto.MarcarTodasLidasResponseDTO;
import com.investai.api.module.notificacao.dto.NotificacaoResponseDTO;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import com.investai.api.module.notificacao.service.NotificacaoService;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificacaoControllerTest {

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

    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(usuarioId);
    }

    @Test
    @DisplayName("GET /notificacoes - deve listar paginado com os parâmetros informados")
    void listar_deveRetornarPagina() throws Exception {
        NotificacaoResponseDTO item = NotificacaoResponseDTO.builder().id(UUID.randomUUID())
                .tipo(TipoNotificacao.SISTEMA).titulo("Aviso").mensagem("m").build();
        when(notificacaoService.listar(usuarioId, false, 1, 5))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/v1/notificacoes").param("lida", "false").param("pagina", "1").param("tamanho", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Aviso"));
    }

    @Test
    @DisplayName("GET /notificacoes/nao-lidas/contagem - deve retornar o total")
    void contar_deveRetornarTotal() throws Exception {
        when(notificacaoService.contarNaoLidas(usuarioId)).thenReturn(new ContagemNaoLidasResponseDTO(7));

        mockMvc.perform(get("/v1/notificacoes/nao-lidas/contagem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7));
    }

    @Test
    @DisplayName("PATCH /notificacoes/{id}/lida - deve retornar 404 quando não existe")
    void marcarLida_deveRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificacaoService.marcarComoLida(usuarioId, id)).thenThrow(new ResourceNotFoundException("Notificação não encontrada"));

        mockMvc.perform(patch("/v1/notificacoes/{id}/lida", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /notificacoes/lidas - deve marcar todas")
    void marcarTodas_deveRetornarQuantidade() throws Exception {
        when(notificacaoService.marcarTodasComoLidas(usuarioId)).thenReturn(new MarcarTodasLidasResponseDTO(2));

        mockMvc.perform(patch("/v1/notificacoes/lidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atualizadas").value(2));
    }

    @Test
    @DisplayName("POST /notificacoes/sistema - deve retornar 201 com o número de destinatários")
    void comunicado_deveRetornar201() throws Exception {
        when(notificacaoService.enviarComunicado(any())).thenReturn(new ComunicadoSistemaResponseDTO(12));

        mockMvc.perform(post("/v1/notificacoes/sistema")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Manutenção\",\"mensagem\":\"Sistema fora do ar às 22h\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destinatarios").value(12));
    }

    @Test
    @DisplayName("POST /notificacoes/sistema - deve retornar 400 sem mensagem")
    void comunicado_deveValidar() throws Exception {
        mockMvc.perform(post("/v1/notificacoes/sistema")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Manutenção\",\"mensagem\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
