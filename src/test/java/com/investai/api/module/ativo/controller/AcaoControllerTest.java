package com.investai.api.module.ativo.controller;

import com.investai.api.infra.exception.*;
import com.investai.api.module.ativo.dto.*;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.service.*;
import com.investai.api.module.auth.service.UsuarioDetailsService;
import com.investai.api.shared.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @MockitoBean
    private CotacaoService cotacaoService;

    @MockitoBean
    private AcaoListagemService acaoListagemService;

    @MockitoBean
    private ComparacaoService comparacaoService;

    @MockitoBean
    private HistoricoService historicoService;

    @MockitoBean
    private AcaoDetalheService acaoDetalheService;

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

    @Test
    @DisplayName("GET /acoes/{codigo}/cotacao - deve retornar cotação com sucesso")
    void obterCotacao_deveRetornar200ComSucesso() throws Exception {
        CotacaoResponseDTO response = CotacaoResponseDTO.builder()
                .codigo("TAEE3")
                .nome("Taesa")
                .setor("Energia Elétrica")
                .preco(BigDecimal.valueOf(38.42))
                .variacaoPercentual(BigDecimal.valueOf(1.25))
                .dividendYield(BigDecimal.valueOf(6.8))
                .precoValorPatrimonial(BigDecimal.valueOf(1.3))
                .volume(24_300_000L)
                .fonte("MOCK")
                .build();

        when(cotacaoService.obterCotacao("TAEE3")).thenReturn(response);

        mockMvc.perform(get("/v1/acoes/{codigo}/cotacao", "TAEE3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("TAEE3"))
                .andExpect(jsonPath("$.preco").value(38.42))
                .andExpect(jsonPath("$.fonte").value("MOCK"));
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/cotacao - deve retornar 404 quando ticker não encontrado")
    void obterCotacao_deveRetornar404QuandoNaoEncontrado() throws Exception {
        when(cotacaoService.obterCotacao("INEXISTENTE"))
                .thenThrow(new ResourceNotFoundException("Nenhuma cotação encontrada para o ticker INEXISTENTE"));

        mockMvc.perform(get("/v1/acoes/{codigo}/cotacao", "INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/cotacao - deve retornar 502 quando HG Brasil está indisponível")
    void obterCotacao_deveRetornar502QuandoIndisponivel() throws Exception {
        when(cotacaoService.obterCotacao("TAEE3"))
                .thenThrow(new HgBrasilIndisponivelException("Serviço de cotações indisponível no momento"));

        mockMvc.perform(get("/v1/acoes/{codigo}/cotacao", "TAEE3"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.erro").value("Serviço de cotações indisponível no momento"));
    }

    @Test
    @DisplayName("GET /acoes - deve retornar página de ativos com sucesso")
    void listar_deveRetornarPaginaComSucesso() throws Exception {
        AcaoListagemResponseDTO dto = AcaoListagemResponseDTO.builder()
                .id(UUID.randomUUID())
                .codigo("TAEE3")
                .nome("Taesa")
                .tipo(TipoAtivo.ACAO)
                .setor("Energia Elétrica")
                .preco(BigDecimal.valueOf(38.42))
                .dividendYield(BigDecimal.valueOf(6.8))
                .cotacaoDisponivel(true)
                .build();

        Page<AcaoListagemResponseDTO> page = new PageImpl<>(List.of(dto));

        when(acaoListagemService.listar(any(AcaoListagemFiltroDTO.class))).thenReturn(page);

        mockMvc.perform(get("/v1/acoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].codigo").value("TAEE3"));
    }

    @Test
    @DisplayName("GET /acoes - deve repassar os filtros de query string corretamente")
    void listar_deveRepassarFiltrosDeQueryStringCorretamente() throws Exception {
        when(acaoListagemService.listar(any(AcaoListagemFiltroDTO.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/v1/acoes")
                        .param("tipo", "ACAO")
                        .param("setor", "Energia")
                        .param("dyMinimo", "6")
                        .param("ordenarPor", "DY")
                        .param("ordem", "DESC")
                        .param("pagina", "2")
                        .param("tamanho", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<AcaoListagemFiltroDTO> captor = ArgumentCaptor.forClass(AcaoListagemFiltroDTO.class);
        verify(acaoListagemService).listar(captor.capture());

        AcaoListagemFiltroDTO filtroCapturado = captor.getValue();
        assertThat(filtroCapturado.getTipo()).containsExactly(TipoAtivo.ACAO);
        assertThat(filtroCapturado.getSetor()).isEqualTo("Energia");
        assertThat(filtroCapturado.getDyMinimo()).isEqualByComparingTo(BigDecimal.valueOf(6));
        assertThat(filtroCapturado.getOrdenarPor()).isEqualTo(OrdenarPorAcao.DY);
        assertThat(filtroCapturado.getOrdem()).isEqualTo(OrdemDTO.DESC);
        assertThat(filtroCapturado.getPagina()).isEqualTo(2);
        assertThat(filtroCapturado.getTamanho()).isEqualTo(5);
    }

    @Test
    @DisplayName("GET /acoes/comparar - deve retornar comparação com sucesso")
    void comparar_deveRetornar200ComSucesso() throws Exception {
        ComparacaoResponseDTO response = ComparacaoResponseDTO.builder()
                .ativos(List.of(
                        AcaoListagemResponseDTO.builder().codigo("TAEE3").nome("Taesa").cotacaoDisponivel(true).build(),
                        AcaoListagemResponseDTO.builder().codigo("PETR4").nome("Petrobras").cotacaoDisponivel(true).build()
                ))
                .build();

        when(comparacaoService.comparar(List.of("TAEE3", "PETR4"))).thenReturn(response);

        mockMvc.perform(get("/v1/acoes/comparar").param("codigos", "TAEE3,PETR4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativos").isArray())
                .andExpect(jsonPath("$.ativos[0].codigo").value("TAEE3"))
                .andExpect(jsonPath("$.ativos[1].codigo").value("PETR4"));
    }

    @Test
    @DisplayName("GET /acoes/comparar - deve retornar 422 quando menos de dois tickers")
    void comparar_deveRetornar422QuandoMenosDeDoisTickers() throws Exception {
        when(comparacaoService.comparar(List.of("TAEE3")))
                .thenThrow(new BusinessException("Informe pelo menos 2 tickers para comparar"));

        mockMvc.perform(get("/v1/acoes/comparar").param("codigos", "TAEE3"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("Informe pelo menos 2 tickers para comparar"));
    }

    @Test
    @DisplayName("GET /acoes/comparar - deve retornar 404 quando ativo não encontrado")
    void comparar_deveRetornar404QuandoAtivoNaoEncontrado() throws Exception {
        when(comparacaoService.comparar(List.of("TAEE3", "NAOEXISTE")))
                .thenThrow(new ResourceNotFoundException("Ativo não cadastrado ou inativo: NAOEXISTE"));

        mockMvc.perform(get("/v1/acoes/comparar").param("codigos", "TAEE3,NAOEXISTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /acoes/comparar - deve retornar 400 quando parâmetro codigos ausente")
    void comparar_deveRetornar400QuandoParametroAusente() throws Exception {
        mockMvc.perform(get("/v1/acoes/comparar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro obrigatório ausente: codigos"));
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/historico - deve retornar série com sucesso")
    void obterHistorico_deveRetornar200ComSucesso() throws Exception {
        HistoricoPrecoResponseDTO response = HistoricoPrecoResponseDTO.builder()
                .codigo("TAEE3")
                .periodo("1M")
                .pontos(List.of(
                        PontoHistoricoDTO.builder()
                                .data(LocalDate.now())
                                .abertura(BigDecimal.valueOf(38.0))
                                .fechamento(BigDecimal.valueOf(38.42))
                                .maxima(BigDecimal.valueOf(38.6))
                                .minima(BigDecimal.valueOf(37.9))
                                .volume(100_000L)
                                .build()
                ))
                .build();

        when(historicoService.obterHistorico("TAEE3", "1M")).thenReturn(response);

        mockMvc.perform(get("/v1/acoes/{codigo}/historico", "TAEE3").param("periodo", "1M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("TAEE3"))
                .andExpect(jsonPath("$.periodo").value("1M"))
                .andExpect(jsonPath("$.pontos").isArray());
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/historico - deve usar período padrão 1M quando não informado")
    void obterHistorico_deveUsarPeriodoPadraoQuandoNaoInformado() throws Exception {
        when(historicoService.obterHistorico("TAEE3", "1M"))
                .thenReturn(HistoricoPrecoResponseDTO.builder().codigo("TAEE3").periodo("1M").pontos(List.of()).build());

        mockMvc.perform(get("/v1/acoes/{codigo}/historico", "TAEE3"))
                .andExpect(status().isOk());

        verify(historicoService).obterHistorico("TAEE3", "1M");
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/historico - deve retornar 422 quando período inválido")
    void obterHistorico_deveRetornar422QuandoPeriodoInvalido() throws Exception {
        when(historicoService.obterHistorico("TAEE3", "2Y"))
                .thenThrow(new BusinessException("Período inválido: 2Y. Valores aceitos: 1S, 1M, 3M, 6M, 1A"));

        mockMvc.perform(get("/v1/acoes/{codigo}/historico", "TAEE3").param("periodo", "2Y"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/historico - deve retornar 404 quando ticker não encontrado")
    void obterHistorico_deveRetornar404QuandoTickerNaoEncontrado() throws Exception {
        when(historicoService.obterHistorico("INEXISTENTE", "1M"))
                .thenThrow(new ResourceNotFoundException("Nenhum histórico encontrado para o ticker INEXISTENTE"));

        mockMvc.perform(get("/v1/acoes/{codigo}/historico", "INEXISTENTE").param("periodo", "1M"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/detalhe - deve retornar detalhe completo com sucesso")
    void obterDetalhe_deveRetornar200ComSucesso() throws Exception {
        AcaoDetalheResponseDTO response = AcaoDetalheResponseDTO.builder()
                .codigo("TAEE3")
                .nome("Taesa")
                .cotacaoDisponivel(true)
                .preco(BigDecimal.valueOf(38.42))
                .precoLucro(null)
                .minimo52Semanas(BigDecimal.valueOf(30.0))
                .maximo52Semanas(BigDecimal.valueOf(45.0))
                .periodoGrafico("1A")
                .pontosGrafico(List.of())
                .glossario(Map.of("DY", "texto explicativo"))
                .build();

        when(acaoDetalheService.obterDetalhe("TAEE3", "1A")).thenReturn(response);

        mockMvc.perform(get("/v1/acoes/{codigo}/detalhe", "TAEE3").param("periodoGrafico", "1A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("TAEE3"))
                .andExpect(jsonPath("$.precoLucro").doesNotExist())
                .andExpect(jsonPath("$.minimo52Semanas").value(30.0))
                .andExpect(jsonPath("$.glossario.DY").exists());
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/detalhe - deve usar 1A como período padrão quando não informado")
    void obterDetalhe_deveUsarPeriodoPadraoQuandoNaoInformado() throws Exception {
        when(acaoDetalheService.obterDetalhe("TAEE3", "1A"))
                .thenReturn(AcaoDetalheResponseDTO.builder().codigo("TAEE3").periodoGrafico("1A").build());

        mockMvc.perform(get("/v1/acoes/{codigo}/detalhe", "TAEE3"))
                .andExpect(status().isOk());

        verify(acaoDetalheService).obterDetalhe("TAEE3", "1A");
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/detalhe - deve retornar 404 quando ativo não encontrado")
    void obterDetalhe_deveRetornar404QuandoNaoEncontrado() throws Exception {
        when(acaoDetalheService.obterDetalhe("NAOEXISTE", "1A"))
                .thenThrow(new ResourceNotFoundException("Ativo não cadastrado ou inativo: NAOEXISTE"));

        mockMvc.perform(get("/v1/acoes/{codigo}/detalhe", "NAOEXISTE").param("periodoGrafico", "1A"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /acoes/{codigo}/detalhe - deve retornar 422 quando período do gráfico inválido")
    void obterDetalhe_deveRetornar422QuandoPeriodoInvalido() throws Exception {
        when(acaoDetalheService.obterDetalhe("TAEE3", "2Y"))
                .thenThrow(new BusinessException("Período inválido: 2Y. Valores aceitos: 1S, 1M, 3M, 6M, 1A"));

        mockMvc.perform(get("/v1/acoes/{codigo}/detalhe", "TAEE3").param("periodoGrafico", "2Y"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /acoes/{id} - busca simples por ID deve continuar funcionando normalmente")
    void buscarPorId_deveContinuarFuncionandoAposMudancaDeRota() throws Exception {
        UUID id = UUID.randomUUID();
        AcaoResponseDTO response = criarResponseMock();

        when(acaoService.buscarPorId(id)).thenReturn(response);

        mockMvc.perform(get("/v1/acoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("TAEE3"));
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