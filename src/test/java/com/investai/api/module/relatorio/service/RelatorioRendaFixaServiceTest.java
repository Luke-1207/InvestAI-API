package com.investai.api.module.relatorio.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.relatorio.dto.RentabilidadePrazoDTO;
import com.investai.api.module.relatorio.dto.TituloRendaFixaRelatorio;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import com.investai.api.module.rendafixa.service.CalculadoraRentabilidadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioRendaFixaServiceTest {

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    private RelatorioRendaFixaService relatorioRendaFixaService;

    @BeforeEach
    void setUp() {
        relatorioRendaFixaService = new RelatorioRendaFixaService(
                tituloPrivadoRepository, tituloTesouroRepository, new CalculadoraRentabilidadeService());
    }

    private TituloPrivado.TituloPrivadoBuilder cdb(UUID id, int diasAteVencimento) {
        return TituloPrivado.builder()
                .id(id)
                .tipo(TipoTituloPrivado.CDB)
                .emissor("Banco Alfa")
                .indexador(Indexador.CDI)
                .taxaPercentual(new BigDecimal("110"))
                .vencimento(LocalDate.now().plusDays(diasAteVencimento))
                .investimentoMinimo(new BigDecimal("1000"))
                .liquidez(TipoLiquidez.NO_VENCIMENTO)
                .garantidoFgc(true)
                .isentoIr(false);
    }

    @Test
    @DisplayName("buscarTitulo - UUID deve buscar título privado e calcular os quatro prazos")
    void buscarTitulo_uuid_deveMontarTituloPrivadoComQuatroPrazos() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(cdb(id, 900).build()));

        TituloRendaFixaRelatorio titulo = relatorioRendaFixaService.buscarTitulo(id.toString());

        assertThat(titulo.nome()).isEqualTo("CDB Banco Alfa");
        assertThat(titulo.identificador()).isEqualTo("CDB-Banco Alfa");
        assertThat(titulo.categoria()).isEqualTo("CDB");
        assertThat(titulo.liquidez()).isEqualTo(RelatorioRendaFixaService.LIQUIDEZ_NO_VENCIMENTO);
        assertThat(titulo.garantia()).isEqualTo(RelatorioRendaFixaService.GARANTIA_FGC);
        assertThat(titulo.taxaSomadaAoIndexador()).isFalse();
        assertThat(titulo.rentabilidades()).extracting(RentabilidadePrazoDTO::prazo)
                .containsExactly("180 dias", "360 dias", "720 dias", "No vencimento (900 dias)");
        assertThat(titulo.rentabilidades()).extracting(r -> r.aliquotaIR().doubleValue())
                .containsExactly(22.5, 20.0, 17.5, 15.0);
        assertThat(titulo.rentabilidades().get(0).taxaLiquida()).isEqualByComparingTo("85.25");
        assertThat(titulo.rentabilidades().get(3).taxaLiquida()).isEqualByComparingTo("93.50");
        verifyNoInteractions(tituloTesouroRepository);
    }

    @Test
    @DisplayName("buscarTitulo - prazos maiores ou iguais ao vencimento não devem aparecer")
    void buscarTitulo_vencimentoCurto_deveOmitirPrazosMaiores() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(cdb(id, 300).build()));

        TituloRendaFixaRelatorio titulo = relatorioRendaFixaService.buscarTitulo(id.toString());

        assertThat(titulo.rentabilidades()).extracting(RentabilidadePrazoDTO::prazo)
                .containsExactly("180 dias", "No vencimento (300 dias)");
        assertThat(titulo.rentabilidades().get(1).aliquotaIR()).isEqualByComparingTo("20.0");
    }

    @Test
    @DisplayName("buscarTitulo - título vencido deve ter só a linha de vencimento com zero dias")
    void buscarTitulo_tituloVencido_deveTerSoLinhaDeVencimento() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(cdb(id, -10).build()));

        TituloRendaFixaRelatorio titulo = relatorioRendaFixaService.buscarTitulo(id.toString());

        assertThat(titulo.rentabilidades()).extracting(RentabilidadePrazoDTO::prazo)
                .containsExactly("No vencimento (0 dias)");
    }

    @Test
    @DisplayName("buscarTitulo - título isento deve ter alíquota zero e líquida igual à bruta")
    void buscarTitulo_tituloIsento_deveTerLiquidaIgualABruta() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(cdb(id, 400)
                .tipo(TipoTituloPrivado.LCI).indexador(Indexador.IPCA).taxaPercentual(new BigDecimal("5.5"))
                .liquidez(TipoLiquidez.DIARIA).garantidoFgc(false).isentoIr(true).build()));

        TituloRendaFixaRelatorio titulo = relatorioRendaFixaService.buscarTitulo(id.toString());

        assertThat(titulo.isentoIr()).isTrue();
        assertThat(titulo.liquidez()).isEqualTo(RelatorioRendaFixaService.LIQUIDEZ_DIARIA);
        assertThat(titulo.garantia()).isEqualTo(RelatorioRendaFixaService.SEM_GARANTIA_FGC);
        assertThat(titulo.taxaSomadaAoIndexador()).isTrue();
        assertThat(titulo.rentabilidades()).allSatisfy(r -> {
            assertThat(r.aliquotaIR()).isEqualByComparingTo("0");
            assertThat(r.taxaLiquida()).isEqualByComparingTo("5.50");
        });
    }

    @Test
    @DisplayName("buscarTitulo - IPCA tributado não deve ter taxa líquida na mesma unidade")
    void buscarTitulo_ipcaTributado_naoDeveTerTaxaLiquida() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(cdb(id, 900)
                .indexador(Indexador.IPCA).taxaPercentual(new BigDecimal("6")).build()));

        TituloRendaFixaRelatorio titulo = relatorioRendaFixaService.buscarTitulo(id.toString());

        assertThat(titulo.rentabilidades()).allSatisfy(r -> assertThat(r.taxaLiquida()).isNull());
    }

    @Test
    @DisplayName("buscarTitulo - código deve buscar título do Tesouro, ignorando espaços")
    void buscarTitulo_codigo_deveMontarTituloDoTesouro() {
        when(tituloTesouroRepository.findByCodigo("SELIC2029")).thenReturn(Optional.of(TituloTesouro.builder()
                .codigo("SELIC2029")
                .nome("Tesouro Selic 2029")
                .tipo(TipoTesouro.SELIC)
                .taxaAnual(new BigDecimal("0.07"))
                .precoMinimo(new BigDecimal("158.33"))
                .vencimento(LocalDate.now().plusDays(1200))
                .build()));

        TituloRendaFixaRelatorio titulo = relatorioRendaFixaService.buscarTitulo(" SELIC2029 ");

        assertThat(titulo.identificador()).isEqualTo("SELIC2029");
        assertThat(titulo.categoria()).isEqualTo(RelatorioRendaFixaService.CATEGORIA_TESOURO);
        assertThat(titulo.indexador()).isEqualTo(Indexador.SELIC);
        assertThat(titulo.garantia()).isEqualTo(RelatorioRendaFixaService.GARANTIA_TESOURO);
        assertThat(titulo.liquidez()).isEqualTo(RelatorioRendaFixaService.LIQUIDEZ_DIARIA);
        assertThat(titulo.investimentoMinimo()).isEqualByComparingTo("158.33");
        assertThat(titulo.isentoIr()).isFalse();
        assertThat(titulo.taxaSomadaAoIndexador()).isTrue();
        assertThat(titulo.rentabilidades()).hasSize(4).allSatisfy(r -> assertThat(r.taxaLiquida()).isNull());
        verifyNoInteractions(tituloPrivadoRepository);
    }

    @Test
    @DisplayName("buscarTitulo - Tesouro prefixado deve ter taxa líquida calculada")
    void buscarTitulo_tesouroPrefixado_deveTerTaxaLiquida() {
        when(tituloTesouroRepository.findByCodigo("PRE2031")).thenReturn(Optional.of(TituloTesouro.builder()
                .codigo("PRE2031")
                .nome("Tesouro Prefixado 2031")
                .tipo(TipoTesouro.PREFIXADO)
                .taxaAnual(new BigDecimal("12"))
                .precoMinimo(new BigDecimal("35"))
                .vencimento(LocalDate.now().plusDays(1500))
                .build()));

        TituloRendaFixaRelatorio titulo = relatorioRendaFixaService.buscarTitulo("PRE2031");

        assertThat(titulo.taxaSomadaAoIndexador()).isFalse();
        assertThat(titulo.rentabilidades().get(3).taxaLiquida()).isEqualByComparingTo("10.20");
    }

    @Test
    @DisplayName("buscarTitulo - UUID inexistente deve lançar ResourceNotFoundException")
    void buscarTitulo_uuidInexistente_deveLancarNotFound() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> relatorioRendaFixaService.buscarTitulo(id.toString()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Título privado não encontrado");
    }

    @Test
    @DisplayName("buscarTitulo - código inexistente deve lançar ResourceNotFoundException")
    void buscarTitulo_codigoInexistente_deveLancarNotFound() {
        when(tituloTesouroRepository.findByCodigo("NADA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> relatorioRendaFixaService.buscarTitulo("NADA"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NADA");
    }
}
