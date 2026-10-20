package com.investai.api.module.busca.service;

import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.busca.dto.GrupoBusca;
import com.investai.api.module.busca.dto.ResultadoBuscaDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuscaAtivosServiceTest {

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @InjectMocks
    private BuscaAtivosService buscaAtivosService;

    private final UUID idCdb = UUID.randomUUID();

    private Acao acao(String codigo, String nome, TipoAtivo tipo, String setor) {
        return Acao.builder().id(UUID.randomUUID()).codigo(codigo).nome(nome).tipo(tipo).setor(setor).ativo(true).build();
    }

    @BeforeEach
    void setUp() {
        when(acaoRepository.findByAtivoTrue()).thenReturn(List.of(
                acao("PETR4", "Petrobras PN", TipoAtivo.ACAO, "Petróleo e Gás"),
                acao("PETR3", "Petrobras ON", TipoAtivo.ACAO, "Petróleo e Gás"),
                acao("VALE3", "Vale ON", TipoAtivo.ACAO, "Mineração"),
                acao("ITUB4", "Itaú Unibanco PN", TipoAtivo.ACAO, "Financeiro"),
                acao("BBAS3", "Banco do Brasil ON", TipoAtivo.ACAO, "Financeiro"),
                acao("HGLG11", "CSHG Logística", TipoAtivo.FII, "Logística"),
                acao("BOVA11", "iShares Ibovespa", TipoAtivo.ETF, null)
        ));
        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(
                TituloTesouro.builder().id(UUID.randomUUID()).codigo("tesouro-ipca-2035").nome("Tesouro IPCA+ 2035")
                        .tipo(TipoTesouro.IPCA).taxaAnual(new BigDecimal("6.20")).vencimento(LocalDate.of(2035, 5, 15)).build(),
                TituloTesouro.builder().id(UUID.randomUUID()).codigo("tesouro-selic-2029").nome("Tesouro Selic 2029")
                        .tipo(TipoTesouro.SELIC).taxaAnual(new BigDecimal("0.05")).vencimento(LocalDate.of(2029, 3, 1)).build()
        ));
        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of(
                TituloPrivado.builder().id(idCdb).tipo(TipoTituloPrivado.CDB).emissor("Banco Inter").indexador(Indexador.CDI)
                        .taxaPercentual(new BigDecimal("110")).vencimento(LocalDate.of(2028, 5, 10))
                        .investimentoMinimo(BigDecimal.valueOf(500)).liquidez(TipoLiquidez.DIARIA).ativo(true).build(),
                TituloPrivado.builder().id(UUID.randomUUID()).tipo(TipoTituloPrivado.LCA).emissor("Banco do Brasil").indexador(Indexador.CDI)
                        .taxaPercentual(new BigDecimal("92")).vencimento(LocalDate.of(2027, 8, 1))
                        .investimentoMinimo(BigDecimal.valueOf(1000)).liquidez(TipoLiquidez.NO_VENCIMENTO).isentoIr(true).ativo(true).build()
        ));
    }

    private List<String> nomes(List<ResultadoBuscaDTO> resultados) {
        return resultados.stream().map(ResultadoBuscaDTO::getNome).toList();
    }

    @Test
    @DisplayName("buscar - código exato deve vir antes dos códigos que só começam igual")
    void buscar_codigoExato_deveVirPrimeiro() {
        List<ResultadoBuscaDTO> resultado = buscaAtivosService.buscar("petr4", 8);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("PETR4");
        assertThat(resultado.get(0).getGrupo()).isEqualTo(GrupoBusca.RENDA_VARIAVEL);
        assertThat(resultado.get(0).getIdentificador()).isEqualTo("PETR4");
        assertThat(resultado.get(0).getDetalhe()).isEqualTo("Petróleo e Gás");
    }

    @Test
    @DisplayName("buscar - prefixo do código deve trazer todos os ativos que começam com ele")
    void buscar_prefixoCodigo_deveTrazerTodos() {
        assertThat(buscaAtivosService.buscar("PETR", 8))
                .extracting(ResultadoBuscaDTO::getCodigo)
                .containsExactly("PETR3", "PETR4");
    }

    @Test
    @DisplayName("buscar - nome sem acento deve encontrar nome com acento")
    void buscar_semAcento_deveEncontrarNomeComAcento() {
        assertThat(nomes(buscaAtivosService.buscar("itau", 8))).containsExactly("Itaú Unibanco PN");
        assertThat(nomes(buscaAtivosService.buscar("LOGÍSTICA", 8))).containsExactly("CSHG Logística");
    }

    @Test
    @DisplayName("buscar - várias palavras devem estar todas no ativo, em qualquer ordem")
    void buscar_variasPalavras_devemEstarTodasPresentes() {
        assertThat(nomes(buscaAtivosService.buscar("selic tesouro", 8))).containsExactly("Tesouro Selic 2029");
        assertThat(nomes(buscaAtivosService.buscar("cdb inter", 8))).containsExactly("CDB - Banco Inter");
    }

    @Test
    @DisplayName("buscar - título do Tesouro deve usar o código como identificador e descrever taxa e vencimento")
    void buscar_tesouro_deveTrazerIdentificadorETaxa() {
        ResultadoBuscaDTO resultado = buscaAtivosService.buscar("ipca", 8).get(0);

        assertThat(resultado.getGrupo()).isEqualTo(GrupoBusca.RENDA_FIXA);
        assertThat(resultado.getCategoria()).isEqualTo("TESOURO");
        assertThat(resultado.getIdentificador()).isEqualTo("tesouro-ipca-2035");
        assertThat(resultado.getCodigo()).isNull();
        assertThat(resultado.getDetalhe()).isEqualTo("IPCA + 6,20% a.a. · vence em 05/2035");
    }

    @Test
    @DisplayName("buscar - título privado deve usar o id como identificador")
    void buscar_tituloPrivado_deveUsarId() {
        ResultadoBuscaDTO resultado = buscaAtivosService.buscar("inter", 8).get(0);

        assertThat(resultado.getCategoria()).isEqualTo("CDB");
        assertThat(resultado.getIdentificador()).isEqualTo(idCdb.toString());
        assertThat(resultado.getDetalhe()).isEqualTo("110,00% do CDI · vence em 05/2028");
    }

    @Test
    @DisplayName("buscar - categoria deve encontrar os ativos dela")
    void buscar_categoria_deveEncontrarAtivosDaCategoria() {
        assertThat(nomes(buscaAtivosService.buscar("fii", 8))).containsExactly("CSHG Logística");
        assertThat(nomes(buscaAtivosService.buscar("lca", 8))).containsExactly("LCA - Banco do Brasil");
    }

    @Test
    @DisplayName("buscar - palavra no começo do nome deve vir antes de palavra no meio")
    void buscar_palavraNoComeco_deveVirAntes() {
        List<ResultadoBuscaDTO> resultado = buscaAtivosService.buscar("banco", 8);

        assertThat(nomes(resultado))
                .containsExactly("Banco do Brasil ON", "CDB - Banco Inter", "LCA - Banco do Brasil", "Itaú Unibanco PN");
    }

    @Test
    @DisplayName("buscar - renda variável deve vir antes da renda fixa quando a relevância empata")
    void buscar_empate_deveColocarRendaVariavelPrimeiro() {
        assertThat(nomes(buscaAtivosService.buscar("brasil", 8)))
                .containsExactly("Banco do Brasil ON", "LCA - Banco do Brasil");
    }

    @Test
    @DisplayName("buscar - deve respeitar o limite de resultados")
    void buscar_deveRespeitarLimite() {
        assertThat(buscaAtivosService.buscar("banco", 2)).hasSize(2);
    }

    @Test
    @DisplayName("buscar - limite nulo deve usar o padrão de 8 resultados")
    void buscar_limiteNulo_deveUsarPadrao() {
        assertThat(buscaAtivosService.buscar("a", null)).isEmpty();
        assertThat(buscaAtivosService.buscar("an", null)).hasSizeLessThanOrEqualTo(8);
    }

    @Test
    @DisplayName("buscar - termo com menos de 2 letras não deve consultar o banco")
    void buscar_termoCurto_naoDeveConsultar() {
        assertThat(buscaAtivosService.buscar(" p ", 8)).isEmpty();

        verifyNoInteractions(acaoRepository, tituloTesouroRepository, tituloPrivadoRepository);
    }

    @Test
    @DisplayName("buscar - termo sem nenhum ativo correspondente deve devolver lista vazia")
    void buscar_semCorrespondencia_deveDevolverVazio() {
        assertThat(buscaAtivosService.buscar("xyz123", 8)).isEmpty();
    }

    @Test
    @DisplayName("normalizar - deve tirar acentos, pontuação e espaços repetidos")
    void normalizar_deveLimparTexto() {
        assertThat(BuscaAtivosService.normalizar("  Itaú   Unibanco-PN ")).isEqualTo("itau unibanco pn");
        assertThat(BuscaAtivosService.normalizar("Tesouro IPCA+ 2035")).isEqualTo("tesouro ipca+ 2035");
        assertThat(BuscaAtivosService.normalizar(null)).isEmpty();
    }
}
