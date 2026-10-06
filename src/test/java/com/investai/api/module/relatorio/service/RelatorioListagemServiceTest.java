package com.investai.api.module.relatorio.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.IaIndisponivelException;
import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.service.AcaoSugestaoService;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.dashboard.dto.SugestoesRendaVariavelResponseDTO;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.service.PerfilService;
import com.investai.api.module.relatorio.dto.DadosRelatorioListagem;
import com.investai.api.module.relatorio.dto.ItemListagemRelatorio;
import com.investai.api.module.relatorio.dto.ModuloRelatorio;
import com.investai.api.module.relatorio.dto.RelatorioListagemRequestDTO;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.service.RendaFixaUnificadaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioListagemServiceTest {

    @Mock
    private PerfilService perfilService;

    @Mock
    private AcaoSugestaoService acaoSugestaoService;

    @Mock
    private RendaFixaUnificadaService rendaFixaUnificadaService;

    @InjectMocks
    private RelatorioListagemService relatorioListagemService;

    private final Usuario usuario = Usuario.builder()
            .id(UUID.randomUUID()).nome("Lucas Silva").email("lucas@email.com").role(Role.USUARIO).build();

    private final UUID idCdb = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(perfilService.obterPerfil(usuario.getId()))
                .thenReturn(PerfilResponseDTO.builder().perfilPreenchido(true).build());
        lenient().when(acaoSugestaoService.listarSugestoes(usuario.getId(), null))
                .thenReturn(SugestoesRendaVariavelResponseDTO.builder()
                        .itens(List.of(acao("TAEE11", 62), acao("ITUB4", 40)))
                        .build());
        lenient().when(rendaFixaUnificadaService.listar("inteligente", usuario.getId()))
                .thenReturn(List.of(
                        rendaFixa(idCdb, null, CategoriaRendaFixa.CDB, "CDB - Banco Alfa", "CDI", "110", 74),
                        rendaFixa(UUID.randomUUID(), "IPCA2035", CategoriaRendaFixa.TESOURO, "Tesouro IPCA+ 2035", "IPCA", "6.2", 55),
                        rendaFixa(UUID.randomUUID(), "SELIC2029", CategoriaRendaFixa.TESOURO, "Tesouro Selic 2029", "SELIC", "0.07", null)));
    }

    private SugestaoAtivoItemDTO acao(String codigo, int score) {
        return SugestaoAtivoItemDTO.builder()
                .codigo(codigo).nome("Empresa " + codigo).tipo(TipoAtivo.ACAO)
                .dy(new BigDecimal("8.4")).score(score)
                .compatibilidade(Compatibilidade.MEDIA).justificativa("Justificativa " + codigo)
                .build();
    }

    private RendaFixaListagemResponseDTO rendaFixa(
            UUID id, String codigo, CategoriaRendaFixa categoria, String nome, String indexador, String taxa, Integer score
    ) {
        return RendaFixaListagemResponseDTO.builder()
                .id(id).codigo(codigo).categoria(categoria).nome(nome).indexador(indexador)
                .taxa(new BigDecimal(taxa)).score(score)
                .compatibilidade(score == null ? null : Compatibilidade.ALTA)
                .justificativa(score == null ? null : "Justificativa " + nome)
                .build();
    }

    private RelatorioListagemRequestDTO request(ModuloRelatorio modulo, String... ativos) {
        return RelatorioListagemRequestDTO.builder()
                .modulo(modulo)
                .ativos(ativos.length == 0 ? null : List.of(ativos))
                .build();
    }

    @Test
    @DisplayName("montarDados - VARIAVEL deve trazer só as ações ranqueadas, com DY como dado principal")
    void montarDados_variavel_deveTrazerAcoes() {
        DadosRelatorioListagem dados = relatorioListagemService.montarDados(request(ModuloRelatorio.VARIAVEL), usuario);

        assertThat(dados.nomeUsuario()).isEqualTo("Lucas Silva");
        assertThat(dados.geradoEm()).isNotNull();
        assertThat(dados.modulo()).isEqualTo(ModuloRelatorio.VARIAVEL);
        assertThat(dados.itens()).extracting(ItemListagemRelatorio::codigo).containsExactly("TAEE11", "ITUB4");
        assertThat(dados.itens().get(0)).satisfies(item -> {
            assertThat(item.nome()).isEqualTo("Empresa TAEE11");
            assertThat(item.tipo()).isEqualTo("Ação");
            assertThat(item.dadoPrincipal()).isEqualTo("DY 8,40%");
            assertThat(item.score()).isEqualTo(62);
            assertThat(item.compatibilidade()).isEqualTo(Compatibilidade.MEDIA);
            assertThat(item.justificativa()).isEqualTo("Justificativa TAEE11");
        });
        assertThat(dados.totalNaListagem()).isEqualTo(2);
        assertThat(dados.filtros()).isEmpty();
        verifyNoInteractions(rendaFixaUnificadaService);
    }

    @Test
    @DisplayName("montarDados - FIXA deve trazer os títulos com a taxa na unidade do indexador")
    void montarDados_fixa_deveTrazerTitulos() {
        DadosRelatorioListagem dados = relatorioListagemService.montarDados(request(ModuloRelatorio.FIXA), usuario);

        assertThat(dados.itens()).extracting(ItemListagemRelatorio::codigo, ItemListagemRelatorio::tipo, ItemListagemRelatorio::dadoPrincipal)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("-", "CDB", "110,00% do CDI"),
                        org.assertj.core.groups.Tuple.tuple("IPCA2035", "Tesouro", "IPCA + 6,20% a.a."),
                        org.assertj.core.groups.Tuple.tuple("SELIC2029", "Tesouro", "Selic + 0,07% a.a."));
        verifyNoInteractions(acaoSugestaoService);
    }

    @Test
    @DisplayName("montarDados - AMBOS deve misturar os dois módulos por score, com itens sem score no fim")
    void montarDados_ambos_deveOrdenarPorScore() {
        DadosRelatorioListagem dados = relatorioListagemService.montarDados(request(ModuloRelatorio.AMBOS), usuario);

        assertThat(dados.itens()).extracting(ItemListagemRelatorio::score)
                .containsExactly(74, 62, 55, 40, null);
        assertThat(dados.itens()).extracting(ItemListagemRelatorio::nome)
                .containsExactly("CDB - Banco Alfa", "Empresa TAEE11", "Tesouro IPCA+ 2035", "Empresa ITUB4", "Tesouro Selic 2029");
    }

    @Test
    @DisplayName("montarDados - com lista de ativos deve incluir só os pedidos e apontar os não encontrados")
    void montarDados_comAtivos_deveFiltrarEApontarNaoEncontrados() {
        DadosRelatorioListagem dados = relatorioListagemService.montarDados(
                request(ModuloRelatorio.AMBOS, " itub4 ", idCdb.toString(), "ipca2035", "XPTO3"), usuario);

        assertThat(dados.itens()).extracting(ItemListagemRelatorio::nome)
                .containsExactly("CDB - Banco Alfa", "Tesouro IPCA+ 2035", "Empresa ITUB4");
        assertThat(dados.totalNaListagem()).isEqualTo(3);
        assertThat(dados.ativosNaoEncontrados()).containsExactly("XPTO3");
    }

    @Test
    @DisplayName("montarDados - sem lista de ativos deve limitar aos 50 mais bem pontuados e informar o total")
    void montarDados_semAtivos_deveLimitarA50() {
        List<SugestaoAtivoItemDTO> muitas = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            muitas.add(acao("ATV" + i, i));
        }
        when(acaoSugestaoService.listarSugestoes(usuario.getId(), null))
                .thenReturn(SugestoesRendaVariavelResponseDTO.builder().itens(muitas).build());

        DadosRelatorioListagem dados = relatorioListagemService.montarDados(request(ModuloRelatorio.VARIAVEL), usuario);

        assertThat(dados.itens()).hasSize(RelatorioListagemRequestDTO.LIMITE_ATIVOS);
        assertThat(dados.itens().get(0).codigo()).isEqualTo("ATV60");
        assertThat(dados.itens().get(49).codigo()).isEqualTo("ATV11");
        assertThat(dados.totalNaListagem()).isEqualTo(60);
    }

    @Test
    @DisplayName("montarDados - deve repassar os filtros na ordem recebida")
    void montarDados_deveRepassarFiltros() {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("Tipo", "Ações");
        filtros.put("Modo", "Inteligente");
        RelatorioListagemRequestDTO request = request(ModuloRelatorio.VARIAVEL);
        request.setFiltros(filtros);

        DadosRelatorioListagem dados = relatorioListagemService.montarDados(request, usuario);

        assertThat(dados.filtros()).containsExactly(Map.entry("Tipo", "Ações"), Map.entry("Modo", "Inteligente"));
    }

    @Test
    @DisplayName("montarDados - perfil não preenchido deve lançar BusinessException sem consultar as listagens")
    void montarDados_perfilNaoPreenchido_deveLancarBusinessException() {
        when(perfilService.obterPerfil(usuario.getId()))
                .thenReturn(PerfilResponseDTO.builder().perfilPreenchido(false).build());

        assertThatThrownBy(() -> relatorioListagemService.montarDados(request(ModuloRelatorio.AMBOS), usuario))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RelatorioListagemService.PERFIL_INCOMPLETO);
        verifyNoInteractions(acaoSugestaoService, rendaFixaUnificadaService);
    }

    @Test
    @DisplayName("montarDados - nenhum ativo pedido encontrado deve lançar BusinessException")
    void montarDados_nenhumAtivoEncontrado_deveLancarBusinessException() {
        assertThatThrownBy(() -> relatorioListagemService.montarDados(request(ModuloRelatorio.VARIAVEL, "XPTO3"), usuario))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RelatorioListagemService.NENHUM_ATIVO);
    }

    @Test
    @DisplayName("montarDados - listagem vazia deve lançar BusinessException")
    void montarDados_listagemVazia_deveLancarBusinessException() {
        when(acaoSugestaoService.listarSugestoes(usuario.getId(), null))
                .thenReturn(SugestoesRendaVariavelResponseDTO.builder().itens(List.of()).build());

        assertThatThrownBy(() -> relatorioListagemService.montarDados(request(ModuloRelatorio.VARIAVEL), usuario))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RelatorioListagemService.NENHUM_ATIVO);
    }

    @Test
    @DisplayName("montarDados - IA indisponível no ranking de renda fixa deve propagar a exceção")
    void montarDados_iaIndisponivel_devePropagar() {
        when(rendaFixaUnificadaService.listar("inteligente", usuario.getId()))
                .thenThrow(new IaIndisponivelException("Serviço de ranqueamento por IA não respondeu a tempo"));

        assertThatThrownBy(() -> relatorioListagemService.montarDados(request(ModuloRelatorio.FIXA), usuario))
                .isInstanceOf(IaIndisponivelException.class);
    }
}
