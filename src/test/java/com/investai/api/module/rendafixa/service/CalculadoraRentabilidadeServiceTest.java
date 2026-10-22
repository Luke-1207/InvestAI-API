package com.investai.api.module.rendafixa.service;

import com.investai.api.module.rendafixa.dto.RentabilidadeEstimadaDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraRentabilidadeServiceTest {

    private final CalculadoraRentabilidadeService calculadoraRentabilidadeService = new CalculadoraRentabilidadeService();

    private TituloPrivado criarTitulo(BigDecimal taxa, LocalDate vencimento, boolean isentoIr) {
        return criarTitulo(Indexador.CDI, taxa, vencimento, isentoIr);
    }

    private TituloPrivado criarTitulo(Indexador indexador, BigDecimal taxa, LocalDate vencimento, boolean isentoIr) {
        return TituloPrivado.builder()
                .id(UUID.randomUUID())
                .tipo(TipoTituloPrivado.CDB)
                .emissor("Banco Teste")
                .indexador(indexador)
                .taxaPercentual(taxa)
                .vencimento(vencimento)
                .investimentoMinimo(BigDecimal.valueOf(500))
                .liquidez(TipoLiquidez.DIARIA)
                .garantidoFgc(true)
                .isentoIr(isentoIr)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("calcular - até 180 dias deve aplicar alíquota de 22,5%")
    void calcular_ate180Dias_deveAplicar22e5PorCento() {
        TituloPrivado titulo = criarTitulo(BigDecimal.valueOf(112.0), LocalDate.now().plusDays(180), false);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("22.5");
    }

    @Test
    @DisplayName("calcular - 181 dias deve cair na faixa de 20%")
    void calcular_181Dias_deveAplicar20PorCento() {
        TituloPrivado titulo = criarTitulo(BigDecimal.valueOf(112.0), LocalDate.now().plusDays(181), false);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("20.0");
    }

    @Test
    @DisplayName("calcular - até 360 dias deve aplicar alíquota de 20%")
    void calcular_ate360Dias_deveAplicar20PorCento() {
        TituloPrivado titulo = criarTitulo(BigDecimal.valueOf(112.0), LocalDate.now().plusDays(360), false);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("20.0");
    }

    @Test
    @DisplayName("calcular - 361 dias deve cair na faixa de 17,5%")
    void calcular_361Dias_deveAplicar17e5PorCento() {
        TituloPrivado titulo = criarTitulo(BigDecimal.valueOf(112.0), LocalDate.now().plusDays(361), false);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("17.5");
    }

    @Test
    @DisplayName("calcular - até 720 dias deve aplicar alíquota de 17,5%")
    void calcular_ate720Dias_deveAplicar17e5PorCento() {
        TituloPrivado titulo = criarTitulo(BigDecimal.valueOf(112.0), LocalDate.now().plusDays(720), false);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("17.5");
    }

    @Test
    @DisplayName("calcular - acima de 720 dias deve aplicar alíquota de 15% e calcular a taxa líquida corretamente")
    void calcular_acima720Dias_deveAplicar15PorCentoECalcularTaxaLiquida() {
        TituloPrivado titulo = criarTitulo(BigDecimal.valueOf(112.0), LocalDate.now().plusDays(721), false);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("15.0");
        assertThat(resultado.getTaxaBrutaAnual()).isEqualByComparingTo("112.0");
        assertThat(resultado.getTaxaLiquidaAnual()).isEqualByComparingTo("95.20"); // 112 × 0.85
    }

    @Test
    @DisplayName("calcular - título isento de IR deve ter alíquota zero e taxa líquida igual à bruta")
    void calcular_tituloIsentoDeIR_deveTerAliquotaZero() {
        TituloPrivado titulo = criarTitulo(BigDecimal.valueOf(6.5), LocalDate.now().plusDays(1000), true);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("0");
        assertThat(resultado.getTaxaLiquidaAnual()).isEqualByComparingTo("6.50");
    }

    @Test
    @DisplayName("calcularAliquotaIR - deve respeitar os limites de cada faixa da tabela regressiva")
    void calcularAliquotaIR_deveRespeitarLimitesDasFaixas() {
        assertThat(calculadoraRentabilidadeService.calcularAliquotaIR(0)).isEqualByComparingTo("22.5");
        assertThat(calculadoraRentabilidadeService.calcularAliquotaIR(180)).isEqualByComparingTo("22.5");
        assertThat(calculadoraRentabilidadeService.calcularAliquotaIR(181)).isEqualByComparingTo("20.0");
        assertThat(calculadoraRentabilidadeService.calcularAliquotaIR(360)).isEqualByComparingTo("20.0");
        assertThat(calculadoraRentabilidadeService.calcularAliquotaIR(361)).isEqualByComparingTo("17.5");
        assertThat(calculadoraRentabilidadeService.calcularAliquotaIR(720)).isEqualByComparingTo("17.5");
        assertThat(calculadoraRentabilidadeService.calcularAliquotaIR(721)).isEqualByComparingTo("15.0");
    }

    @Test
    @DisplayName("calcularParaPrazo - deve aplicar a alíquota do prazo informado sobre a taxa bruta")
    void calcularParaPrazo_deveAplicarAliquotaDoPrazo() {
        RentabilidadeEstimadaDTO resultado =
                calculadoraRentabilidadeService.calcularParaPrazo(BigDecimal.valueOf(110), false, 180);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("22.5");
        assertThat(resultado.getTaxaBrutaAnual()).isEqualByComparingTo("110");
        assertThat(resultado.getTaxaLiquidaAnual()).isEqualByComparingTo("85.25");
    }

    @Test
    @DisplayName("calcularParaPrazo - isento de IR deve manter a taxa em qualquer prazo")
    void calcularParaPrazo_isento_deveManterTaxa() {
        RentabilidadeEstimadaDTO resultado =
                calculadoraRentabilidadeService.calcularParaPrazo(BigDecimal.valueOf(95), true, 90);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("0");
        assertThat(resultado.getTaxaLiquidaAnual()).isEqualByComparingTo("95.00");
    }

    private TituloTesouro criarTesouro(TipoTesouro tipo, BigDecimal taxa) {
        return TituloTesouro.builder()
                .id(UUID.randomUUID())
                .codigo("tesouro-" + tipo.name().toLowerCase())
                .nome("Tesouro " + tipo.name())
                .tipo(tipo)
                .taxaAnual(taxa)
                .precoMinimo(BigDecimal.valueOf(100))
                .vencimento(LocalDate.now().plusDays(1000))
                .build();
    }

    @Test
    @DisplayName("calcular - CDB IPCA+ não deve descontar o IR só da parte fixa e deve informar a parcela líquida")
    void calcular_tituloIpca_deveInformarParcelaLiquidaSemTaxaLiquida() {
        TituloPrivado titulo = criarTitulo(Indexador.IPCA, BigDecimal.valueOf(6.5), LocalDate.now().plusDays(1000), false);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.isTaxaSomadaAoIndexador()).isTrue();
        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("15.0");
        assertThat(resultado.getTaxaLiquidaAnual()).isNull();
        assertThat(resultado.getPercentualRendimentoLiquido()).isEqualByComparingTo("85.0");
    }

    @Test
    @DisplayName("calcular - LCI IPCA+ isenta deve manter a taxa líquida igual à bruta")
    void calcular_tituloIpcaIsento_deveManterTaxaLiquida() {
        TituloPrivado titulo = criarTitulo(Indexador.IPCA, BigDecimal.valueOf(5.2), LocalDate.now().plusDays(1000), true);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.isTaxaSomadaAoIndexador()).isTrue();
        assertThat(resultado.getTaxaLiquidaAnual()).isEqualByComparingTo("5.20");
        assertThat(resultado.getPercentualRendimentoLiquido()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("calcular - título CDI deve manter a taxa líquida em % do CDI")
    void calcular_tituloCdi_deveManterTaxaLiquida() {
        TituloPrivado titulo = criarTitulo(BigDecimal.valueOf(110), LocalDate.now().plusDays(1000), false);

        RentabilidadeEstimadaDTO resultado = calculadoraRentabilidadeService.calcular(titulo);

        assertThat(resultado.isTaxaSomadaAoIndexador()).isFalse();
        assertThat(resultado.getTaxaLiquidaAnual()).isEqualByComparingTo("93.50");
        assertThat(resultado.getPercentualRendimentoLiquido()).isEqualByComparingTo("85.0");
    }

    @Test
    @DisplayName("calcular - Tesouro Selic e IPCA+ devem informar a parcela líquida em vez da taxa líquida")
    void calcular_tesouroSelicEIpca_deveInformarParcelaLiquida() {
        for (TipoTesouro tipo : new TipoTesouro[]{TipoTesouro.SELIC, TipoTesouro.IPCA}) {
            RentabilidadeEstimadaDTO resultado =
                    calculadoraRentabilidadeService.calcular(criarTesouro(tipo, BigDecimal.valueOf(0.08)));

            assertThat(resultado.isTaxaSomadaAoIndexador()).isTrue();
            assertThat(resultado.getTaxaLiquidaAnual()).isNull();
            assertThat(resultado.getPercentualRendimentoLiquido()).isEqualByComparingTo("85.0");
        }
    }

    @Test
    @DisplayName("calcular - Tesouro Prefixado deve trazer a taxa líquida ao ano")
    void calcular_tesouroPrefixado_deveTrazerTaxaLiquida() {
        RentabilidadeEstimadaDTO resultado =
                calculadoraRentabilidadeService.calcular(criarTesouro(TipoTesouro.PREFIXADO, BigDecimal.valueOf(12)));

        assertThat(resultado.isTaxaSomadaAoIndexador()).isFalse();
        assertThat(resultado.getTaxaLiquidaAnual()).isEqualByComparingTo("10.20");
    }

    @Test
    @DisplayName("calcularParaPrazo - taxa somada ao indexador sem isenção não deve ter taxa líquida")
    void calcularParaPrazo_taxaSomada_naoDeveTerTaxaLiquida() {
        RentabilidadeEstimadaDTO resultado =
                calculadoraRentabilidadeService.calcularParaPrazo(BigDecimal.valueOf(6), false, 200, true);

        assertThat(resultado.getAliquotaIR()).isEqualByComparingTo("20.0");
        assertThat(resultado.getTaxaLiquidaAnual()).isNull();
        assertThat(resultado.getPercentualRendimentoLiquido()).isEqualByComparingTo("80.0");
    }
}