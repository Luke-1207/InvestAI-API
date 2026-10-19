package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.dashboard.entity.IndicadoresMercado;
import com.investai.api.module.dashboard.repository.IndicadoresMercadoRepository;
import com.investai.api.module.rendafixa.dto.SimulacaoRendimentoRequestDTO;
import com.investai.api.module.rendafixa.dto.SimulacaoRendimentoResponseDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimuladorRendimentoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 19);
    private static final LocalDateTime SINCRONIZADO_EM = LocalDateTime.of(2026, 10, 19, 8, 0);

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @Mock
    private IndicadoresMercadoRepository indicadoresMercadoRepository;

    @Spy
    private CalculadoraRentabilidadeService calculadoraRentabilidadeService = new CalculadoraRentabilidadeService();

    @InjectMocks
    private SimuladorRendimentoService simuladorRendimentoService;

    private void indicadores(String selic, String ipca) {
        when(indicadoresMercadoRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(IndicadoresMercado.builder()
                .selicAtual(selic == null ? null : new BigDecimal(selic))
                .ipcaAcumulado12m(ipca == null ? null : new BigDecimal(ipca))
                .selicIpcaSincronizadoEm(SINCRONIZADO_EM)
                .build()));
    }

    private TituloPrivado privado(UUID id, Indexador indexador, String taxa, boolean isento, TipoLiquidez liquidez, LocalDate vencimento) {
        TituloPrivado titulo = TituloPrivado.builder()
                .id(id).tipo(isento ? TipoTituloPrivado.LCI : TipoTituloPrivado.CDB).emissor("Banco Teste")
                .indexador(indexador).taxaPercentual(new BigDecimal(taxa)).vencimento(vencimento)
                .investimentoMinimo(new BigDecimal("100.00")).liquidez(liquidez)
                .garantidoFgc(true).isentoIr(isento).ativo(true)
                .build();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(titulo));
        return titulo;
    }

    private void tesouro(String codigo, TipoTesouro tipo, String taxa, LocalDate vencimento) {
        when(tituloTesouroRepository.findByCodigo(codigo)).thenReturn(Optional.of(TituloTesouro.builder()
                .id(UUID.randomUUID()).codigo(codigo).nome("Tesouro Teste").tipo(tipo)
                .taxaAnual(new BigDecimal(taxa)).precoMinimo(new BigDecimal("35.00"))
                .vencimento(vencimento).disponivel(true)
                .build()));
    }

    private SimulacaoRendimentoRequestDTO pedido(String valor, int meses) {
        return new SimulacaoRendimentoRequestDTO(new BigDecimal(valor), meses);
    }

    @Test
    @DisplayName("CDB de 110% do CDI: usa o CDI estimado pela Selic, aplica o IR regressivo e compara com a poupança")
    void simular_cdbPercentualDoCdi() {
        UUID id = UUID.randomUUID();
        indicadores("15.00", "4.62");
        privado(id, Indexador.CDI, "110", false, TipoLiquidez.DIARIA, HOJE.plusYears(3));

        SimulacaoRendimentoResponseDTO r = simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", 12), HOJE);

        assertThat(r.getDataResgate()).isEqualTo(LocalDate.of(2027, 10, 19));
        assertThat(r.getPrazoDias()).isEqualTo(365);
        assertThat(r.getTaxaAnualEstimada()).isEqualByComparingTo("16.39");
        assertThat(r.getValorBruto()).isEqualByComparingTo("1163.90");
        assertThat(r.getRendimentoBruto()).isEqualByComparingTo("163.90");
        assertThat(r.getAliquotaIR()).isEqualByComparingTo("17.5");
        assertThat(r.getImpostoRenda()).isEqualByComparingTo("28.68");
        assertThat(r.getValorLiquido()).isEqualByComparingTo("1135.22");
        assertThat(r.getRendimentoLiquido()).isEqualByComparingTo("135.22");
        assertThat(r.getValorPoupanca()).isEqualByComparingTo("1061.68");
        assertThat(r.getRendimentoPoupanca()).isEqualByComparingTo("61.68");
        assertThat(r.getTaxaAnualPoupanca()).isEqualByComparingTo("6.17");
        assertThat(r.getDiferencaParaPoupanca()).isEqualByComparingTo("73.54");
        assertThat(r.getIndicadoresAtualizadosEm()).isEqualTo(SINCRONIZADO_EM);
        assertThat(r.getAvisos()).isEmpty();
        assertThat(r.getPremissas()).containsExactly(
                "Selic atual de 15,00% ao ano.",
                "CDI estimado em 14,90% ao ano, um pouco abaixo da Selic.",
                "Rendimento do título mantido em 16,39% ao ano durante todo o prazo.",
                "Imposto de Renda de 17,5% sobre o rendimento, pela tabela regressiva para um resgate em 365 dias.",
                "Poupança rendendo 0,5% ao mês, regra para quando a Selic está acima de 8,5% ao ano, sem somar a TR.",
                "Não considera IOF, taxa de custódia nem outras tarifas.");
    }

    @Test
    @DisplayName("LCI isenta: o valor líquido é igual ao bruto e a premissa explica a isenção")
    void simular_tituloIsento() {
        UUID id = UUID.randomUUID();
        indicadores("15.00", null);
        privado(id, Indexador.CDI, "95", true, TipoLiquidez.DIARIA, HOJE.plusYears(2));

        SimulacaoRendimentoResponseDTO r = simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", 12), HOJE);

        assertThat(r.getAliquotaIR()).isEqualByComparingTo("0");
        assertThat(r.getImpostoRenda()).isEqualByComparingTo("0");
        assertThat(r.getValorLiquido()).isEqualByComparingTo(r.getValorBruto()).isEqualByComparingTo("1141.55");
        assertThat(r.getDiferencaParaPoupanca()).isEqualByComparingTo("79.87");
        assertThat(r.getPremissas()).contains("Título isento de Imposto de Renda para pessoa física.");
    }

    @Test
    @DisplayName("Tesouro IPCA+: soma a inflação dos últimos 12 meses ao juro real e avisa sobre a venda antes do vencimento")
    void simular_tesouroIpca() {
        indicadores("15.00", "4.62");
        tesouro("IPCA2035", TipoTesouro.IPCA, "7.22", LocalDate.of(2035, 5, 15));

        SimulacaoRendimentoResponseDTO r = simuladorRendimentoService.simularTesouro("IPCA2035", pedido("1000", 24), HOJE);

        assertThat(r.getTaxaAnualEstimada()).isEqualByComparingTo("12.17");
        assertThat(r.getValorBruto()).isEqualByComparingTo("1258.29");
        assertThat(r.getAliquotaIR()).isEqualByComparingTo("15.0");
        assertThat(r.getImpostoRenda()).isEqualByComparingTo("38.74");
        assertThat(r.getValorLiquido()).isEqualByComparingTo("1219.55");
        assertThat(r.getValorPoupanca()).isEqualByComparingTo("1127.16");
        assertThat(r.getPremissas()).contains("Inflação de 4,62% ao ano, igual ao IPCA dos últimos 12 meses.");
        assertThat(r.getAvisos()).singleElement().asString().contains("preço de mercado do dia");
    }

    @Test
    @DisplayName("Tesouro Selic: a taxa do título é somada à Selic e não há aviso de preço de mercado")
    void simular_tesouroSelic() {
        indicadores("15.00", "4.62");
        tesouro("SELIC2029", TipoTesouro.SELIC, "0.08", LocalDate.of(2029, 3, 1));

        SimulacaoRendimentoResponseDTO r = simuladorRendimentoService.simularTesouro("SELIC2029", pedido("1000", 12), HOJE);

        assertThat(r.getTaxaAnualEstimada()).isEqualByComparingTo("15.09");
        assertThat(r.getValorBruto()).isEqualByComparingTo("1150.92");
        assertThat(r.getValorLiquido()).isEqualByComparingTo("1124.51");
        assertThat(r.getAvisos()).isEmpty();
    }

    @Test
    @DisplayName("Prefixado: usa a taxa contratada direto, sem depender de indicador")
    void simular_prefixado() {
        indicadores("15.00", null);
        tesouro("PRE2031", TipoTesouro.PREFIXADO, "14.36", LocalDate.of(2031, 1, 1));

        SimulacaoRendimentoResponseDTO r = simuladorRendimentoService.simularTesouro("PRE2031", pedido("5000", 6), HOJE);

        assertThat(r.getTaxaAnualEstimada()).isEqualByComparingTo("14.36");
        assertThat(r.getPrazoDias()).isEqualTo(182);
        assertThat(r.getAliquotaIR()).isEqualByComparingTo("20.0");
        assertThat(r.getValorBruto()).isEqualByComparingTo("5346.96");
        assertThat(r.getImpostoRenda()).isEqualByComparingTo("69.39");
        assertThat(r.getValorLiquido()).isEqualByComparingTo("5277.57");
        assertThat(r.getValorPoupanca()).isEqualByComparingTo("5151.89");
    }

    @Test
    @DisplayName("Com a Selic em até 8,5%, a poupança passa a render 70% da Selic")
    void simular_poupancaComSelicBaixa() {
        UUID id = UUID.randomUUID();
        indicadores("8.00", null);
        privado(id, Indexador.PREFIXADO, "12", false, TipoLiquidez.DIARIA, HOJE.plusYears(2));

        SimulacaoRendimentoResponseDTO r = simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", 12), HOJE);

        assertThat(r.getValorPoupanca()).isEqualByComparingTo("1056.00");
        assertThat(r.getTaxaAnualPoupanca()).isEqualByComparingTo("5.60");
        assertThat(r.getDiferencaParaPoupanca()).isEqualByComparingTo("43.00");
        assertThat(r.getPremissas()).contains(
                "Poupança rendendo 70% da Selic, regra para quando a Selic está em até 8,5% ao ano, sem somar a TR.");
    }

    @ParameterizedTest(name = "{0} meses ({1} dias) deve usar alíquota de {2}%")
    @CsvSource({"3, 92, 22.5", "6, 182, 20.0", "12, 365, 17.5", "24, 731, 15.0"})
    @DisplayName("A alíquota de IR segue a tabela regressiva pelo prazo do resgate")
    void simular_aliquotaPelaTabelaRegressiva(int meses, long dias, String aliquota) {
        UUID id = UUID.randomUUID();
        indicadores("15.00", null);
        privado(id, Indexador.PREFIXADO, "12", false, TipoLiquidez.DIARIA, HOJE.plusYears(5));

        SimulacaoRendimentoResponseDTO r = simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", meses), HOJE);

        assertThat(r.getPrazoDias()).isEqualTo(dias);
        assertThat(r.getAliquotaIR()).isEqualByComparingTo(aliquota);
    }

    @Test
    @DisplayName("Título com resgate só no vencimento: avisa quando o prazo termina antes do vencimento")
    void simular_avisaResgateSoNoVencimento() {
        UUID id = UUID.randomUUID();
        indicadores("15.00", null);
        privado(id, Indexador.CDI, "110", false, TipoLiquidez.NO_VENCIMENTO, LocalDate.of(2028, 5, 10));

        SimulacaoRendimentoResponseDTO r = simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", 12), HOJE);

        assertThat(r.getAvisos()).singleElement().asString()
                .startsWith("Este título só pode ser resgatado no vencimento, em 10/05/2028.");
    }

    @Test
    @DisplayName("Título com resgate só no vencimento: não avisa quando o prazo termina exatamente no vencimento")
    void simular_semAvisoQuandoPrazoCoincideComVencimento() {
        UUID id = UUID.randomUUID();
        indicadores("15.00", null);
        privado(id, Indexador.CDI, "110", false, TipoLiquidez.NO_VENCIMENTO, HOJE.plusMonths(12));

        SimulacaoRendimentoResponseDTO r = simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", 12), HOJE);

        assertThat(r.getAvisos()).isEmpty();
    }

    @Test
    @DisplayName("Prazo além do vencimento deve ser recusado dizendo até quantos meses dá para simular")
    void simular_prazoAlemDoVencimento() {
        UUID id = UUID.randomUUID();
        indicadores("15.00", null);
        privado(id, Indexador.CDI, "110", false, TipoLiquidez.DIARIA, LocalDate.of(2028, 5, 10));

        assertThatThrownBy(() -> simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", 24), HOJE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O prazo passa do vencimento do título, em 10/05/2028. Escolha até 18 meses.");
    }

    @Test
    @DisplayName("Título que vence em menos de um mês não pode ser simulado")
    void simular_tituloVencendoEmMenosDeUmMes() {
        UUID id = UUID.randomUUID();
        indicadores("15.00", null);
        privado(id, Indexador.CDI, "110", false, TipoLiquidez.DIARIA, HOJE.plusDays(10));

        assertThatThrownBy(() -> simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", 1), HOJE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Este título vence em 29/10/2026, antes de completar um mês. Não dá para simular esse prazo.");
    }

    @Test
    @DisplayName("Valor abaixo do investimento mínimo deve ser recusado")
    void simular_valorAbaixoDoMinimo() {
        UUID id = UUID.randomUUID();
        indicadores("15.00", null);
        privado(id, Indexador.CDI, "110", false, TipoLiquidez.DIARIA, HOJE.plusYears(2));

        assertThatThrownBy(() -> simuladorRendimentoService.simularTituloPrivado(id, pedido("50", 12), HOJE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O investimento mínimo deste título é R$ 100,00.");
    }

    @Test
    @DisplayName("Sem Selic carregada, a simulação não é feita")
    void simular_semSelic() {
        UUID id = UUID.randomUUID();
        indicadores(null, "4.62");
        privado(id, Indexador.PREFIXADO, "12", false, TipoLiquidez.DIARIA, HOJE.plusYears(2));

        assertThatThrownBy(() -> simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", 12), HOJE))
                .isInstanceOf(BusinessException.class)
                .hasMessage(SimuladorRendimentoService.MENSAGEM_SEM_INDICADORES);
    }

    @Test
    @DisplayName("Título atrelado ao IPCA sem IPCA carregado não é simulado")
    void simular_ipcaSemIndicador() {
        indicadores("15.00", null);
        tesouro("IPCA2035", TipoTesouro.IPCA, "7.22", LocalDate.of(2035, 5, 15));

        assertThatThrownBy(() -> simuladorRendimentoService.simularTesouro("IPCA2035", pedido("1000", 12), HOJE))
                .isInstanceOf(BusinessException.class)
                .hasMessage(SimuladorRendimentoService.MENSAGEM_SEM_INDICADORES);
    }

    @Test
    @DisplayName("Título inexistente deve gerar 404")
    void simular_tituloInexistente() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.empty());
        when(tituloTesouroRepository.findByCodigo("NAOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> simuladorRendimentoService.simularTituloPrivado(id, pedido("1000", 12), HOJE))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> simuladorRendimentoService.simularTesouro("NAOEXISTE", pedido("1000", 12), HOJE))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
