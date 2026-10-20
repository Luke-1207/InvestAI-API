package com.investai.api.module.busca.controller;

import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.busca.dto.GrupoBusca;
import com.investai.api.module.busca.dto.ResultadoBuscaDTO;
import com.investai.api.module.busca.service.BuscaAtivosService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BuscaAtivosController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BuscaAtivosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuscaAtivosService buscaAtivosService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("GET /v1/busca - deve devolver os resultados com o limite padrão de 8")
    void buscar_semLimite_deveUsarPadrao() throws Exception {
        when(buscaAtivosService.buscar("petr", 8)).thenReturn(List.of(ResultadoBuscaDTO.builder()
                .grupo(GrupoBusca.RENDA_VARIAVEL).categoria("ACAO").identificador("PETR4").codigo("PETR4")
                .nome("Petrobras PN").detalhe("Petróleo e Gás").build()));

        mockMvc.perform(get("/v1/busca").param("termo", "petr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grupo").value("RENDA_VARIAVEL"))
                .andExpect(jsonPath("$[0].identificador").value("PETR4"))
                .andExpect(jsonPath("$[0].nome").value("Petrobras PN"));
    }

    @Test
    @DisplayName("GET /v1/busca - deve repassar o limite informado")
    void buscar_comLimite_deveRepassar() throws Exception {
        when(buscaAtivosService.buscar("tesouro", 3)).thenReturn(List.of());

        mockMvc.perform(get("/v1/busca").param("termo", "tesouro").param("limite", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /v1/busca - sem termo deve devolver 400 com a mensagem do campo")
    void buscar_semTermo_deveDevolver400() throws Exception {
        mockMvc.perform(get("/v1/busca"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.termo").value("Informe o que você quer buscar."));

        verifyNoInteractions(buscaAtivosService);
    }

    @Test
    @DisplayName("GET /v1/busca - limite acima de 20 deve devolver 400")
    void buscar_limiteAlto_deveDevolver400() throws Exception {
        mockMvc.perform(get("/v1/busca").param("termo", "petr").param("limite", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.limite").value("O limite pode ser de no máximo 20 resultados."));
    }

    @Test
    @DisplayName("GET /v1/busca - termo com mais de 60 caracteres deve devolver 400")
    void buscar_termoLongo_deveDevolver400() throws Exception {
        mockMvc.perform(get("/v1/busca").param("termo", "a".repeat(61)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.termo").value("A busca pode ter no máximo 60 caracteres."));
    }
}
