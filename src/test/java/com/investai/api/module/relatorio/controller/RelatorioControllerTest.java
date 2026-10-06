package com.investai.api.module.relatorio.controller;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.GlobalExceptionHandler;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.module.relatorio.dto.HistoricoRelatorioResponseDTO;
import com.investai.api.module.relatorio.dto.ModuloRelatorio;
import com.investai.api.module.relatorio.dto.RelatorioGeradoDTO;
import com.investai.api.module.relatorio.dto.RelatorioListagemRequestDTO;
import com.investai.api.module.relatorio.entity.TipoRelatorio;
import com.investai.api.module.relatorio.service.HistoricoRelatorioService;
import com.investai.api.module.relatorio.service.RelatorioService;
import com.investai.api.shared.security.JwtUtil;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private HistoricoRelatorioService historicoRelatorioService;

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
        when(usuarioAutenticadoHelper.getIdUsuarioLogado()).thenReturn(usuario.getId());
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
    @DisplayName("POST /relatorios/listagem - deve retornar o PDF com headers de download")
    void gerarRelatorioListagem_deveRetornarPdfComHeadersDeDownload() throws Exception {
        when(relatorioService.gerarRelatorioListagem(any(RelatorioListagemRequestDTO.class), eq(usuario)))
                .thenReturn(new RelatorioGeradoDTO("listagem-renda-variavel.pdf", PDF));

        mockMvc.perform(post("/v1/relatorios/listagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modulo": "VARIAVEL",
                                  "filtros": { "Tipo": "Ações" },
                                  "ativos": ["TAEE11", "ITUB4"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"listagem-renda-variavel.pdf\""))
                .andExpect(content().bytes(PDF));

        ArgumentCaptor<RelatorioListagemRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(RelatorioListagemRequestDTO.class);
        verify(relatorioService).gerarRelatorioListagem(requestCaptor.capture(), eq(usuario));
        assertThat(requestCaptor.getValue().getModulo()).isEqualTo(ModuloRelatorio.VARIAVEL);
        assertThat(requestCaptor.getValue().getFiltros()).containsEntry("Tipo", "Ações");
        assertThat(requestCaptor.getValue().getAtivos()).containsExactly("TAEE11", "ITUB4");
    }

    @Test
    @DisplayName("POST /relatorios/listagem - só o módulo é obrigatório")
    void gerarRelatorioListagem_soModulo_deveAceitar() throws Exception {
        when(relatorioService.gerarRelatorioListagem(any(RelatorioListagemRequestDTO.class), eq(usuario)))
                .thenReturn(new RelatorioGeradoDTO("listagem-completa.pdf", PDF));

        mockMvc.perform(post("/v1/relatorios/listagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modulo\": \"AMBOS\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /relatorios/listagem - sem módulo deve retornar 400")
    void gerarRelatorioListagem_semModulo_deveRetornar400() throws Exception {
        mockMvc.perform(post("/v1/relatorios/listagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativos\": [\"TAEE11\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.modulo").exists());

        verifyNoInteractions(relatorioService);
    }

    @Test
    @DisplayName("POST /relatorios/listagem - módulo inválido deve retornar 400")
    void gerarRelatorioListagem_moduloInvalido_deveRetornar400() throws Exception {
        mockMvc.perform(post("/v1/relatorios/listagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modulo\": \"CRIPTO\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(relatorioService);
    }

    @Test
    @DisplayName("POST /relatorios/listagem - mais de 50 ativos deve retornar 400")
    void gerarRelatorioListagem_maisDe50Ativos_deveRetornar400() throws Exception {
        String ativos = IntStream.rangeClosed(1, 51)
                .mapToObj(i -> "\"ATV" + i + "\"")
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/v1/relatorios/listagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modulo\": \"VARIAVEL\", \"ativos\": [" + ativos + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.ativos").value("Informe no máximo 50 ativos por relatório"));

        verifyNoInteractions(relatorioService);
    }

    @Test
    @DisplayName("POST /relatorios/listagem - exatamente 50 ativos deve ser aceito")
    void gerarRelatorioListagem_cinquentaAtivos_deveAceitar() throws Exception {
        when(relatorioService.gerarRelatorioListagem(any(RelatorioListagemRequestDTO.class), eq(usuario)))
                .thenReturn(new RelatorioGeradoDTO("listagem-renda-variavel.pdf", PDF));
        String ativos = IntStream.rangeClosed(1, 50)
                .mapToObj(i -> "\"ATV" + i + "\"")
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/v1/relatorios/listagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modulo\": \"VARIAVEL\", \"ativos\": [" + ativos + "]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /relatorios/listagem - código de ativo em branco deve retornar 400")
    void gerarRelatorioListagem_codigoEmBranco_deveRetornar400() throws Exception {
        mockMvc.perform(post("/v1/relatorios/listagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modulo\": \"VARIAVEL\", \"ativos\": [\"TAEE11\", \" \"]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(relatorioService);
    }

    @Test
    @DisplayName("POST /relatorios/listagem - perfil incompleto deve retornar 422")
    void gerarRelatorioListagem_perfilIncompleto_deveRetornar422() throws Exception {
        when(relatorioService.gerarRelatorioListagem(any(RelatorioListagemRequestDTO.class), eq(usuario)))
                .thenThrow(new BusinessException("Complete seu perfil de investidor para gerar o relatório de listagem ranqueada"));

        mockMvc.perform(post("/v1/relatorios/listagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modulo\": \"VARIAVEL\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /relatorios/perfil - deve retornar o PDF do perfil com headers de download")
    void gerarRelatorioPerfil_deveRetornarPdfComHeadersDeDownload() throws Exception {
        when(relatorioService.gerarRelatorioPerfil(usuario))
                .thenReturn(new RelatorioGeradoDTO("perfil-investidor.pdf", PDF));

        mockMvc.perform(get("/v1/relatorios/perfil"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"perfil-investidor.pdf\""))
                .andExpect(content().bytes(PDF));
    }

    @Test
    @DisplayName("GET /relatorios/perfil - perfil inexistente deve retornar 404")
    void gerarRelatorioPerfil_perfilInexistente_deveRetornar404() throws Exception {
        when(relatorioService.gerarRelatorioPerfil(usuario))
                .thenThrow(new ResourceNotFoundException("Perfil do investidor não encontrado"));

        mockMvc.perform(get("/v1/relatorios/perfil"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Perfil do investidor não encontrado"));
    }

    @Test
    @DisplayName("GET /relatorios/historico - deve retornar a página de registros do usuário logado")
    void listarHistorico_deveRetornarPaginaDeRegistros() throws Exception {
        UUID idRegistro = UUID.randomUUID();
        when(historicoRelatorioService.listar(usuario.getId(), 0, 20))
                .thenReturn(new PageImpl<>(
                        List.of(HistoricoRelatorioResponseDTO.builder()
                                .id(idRegistro)
                                .tipo(TipoRelatorio.ATIVO_INDIVIDUAL)
                                .referencia("TAEE11")
                                .geradoEm(LocalDateTime.of(2026, 10, 6, 15, 0))
                                .build()),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/v1/relatorios/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(idRegistro.toString()))
                .andExpect(jsonPath("$.content[0].tipo").value("ATIVO_INDIVIDUAL"))
                .andExpect(jsonPath("$.content[0].referencia").value("TAEE11"))
                .andExpect(jsonPath("$.content[0].geradoEm").value("2026-10-06T15:00:00"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verifyNoInteractions(relatorioService);
    }

    @Test
    @DisplayName("GET /relatorios/historico - deve repassar página e tamanho informados")
    void listarHistorico_deveRepassarPaginacao() throws Exception {
        when(historicoRelatorioService.listar(usuario.getId(), 2, 5))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get("/v1/relatorios/historico").param("pagina", "2").param("tamanho", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(historicoRelatorioService).listar(usuario.getId(), 2, 5);
    }

    @Test
    @DisplayName("GET /relatorios/historico - página não numérica deve retornar 400")
    void listarHistorico_paginaNaoNumerica_deveRetornar400() throws Exception {
        mockMvc.perform(get("/v1/relatorios/historico").param("pagina", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(historicoRelatorioService);
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
