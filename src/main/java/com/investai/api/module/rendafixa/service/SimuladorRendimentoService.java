package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.dashboard.entity.IndicadoresMercado;
import com.investai.api.module.dashboard.repository.IndicadoresMercadoRepository;
import com.investai.api.module.rendafixa.dto.SimulacaoRendimentoRequestDTO;
import com.investai.api.module.rendafixa.dto.SimulacaoRendimentoResponseDTO;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SimuladorRendimentoService {

    static final double DIFERENCA_CDI_SELIC = 0.10;
    static final double LIMITE_SELIC_POUPANCA = 8.5;
    static final double POUPANCA_MENSAL_SELIC_ALTA = 0.005;
    static final double FRACAO_SELIC_POUPANCA = 0.70;

    static final String MENSAGEM_SEM_INDICADORES =
            "Os indicadores de mercado ainda não foram carregados. Tente de novo em alguns minutos.";

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TituloPrivadoRepository tituloPrivadoRepository;
    private final TituloTesouroRepository tituloTesouroRepository;
    private final IndicadoresMercadoRepository indicadoresMercadoRepository;
    private final CalculadoraRentabilidadeService calculadoraRentabilidadeService;

    enum Remuneracao {
        PERCENTUAL_CDI,
        PERCENTUAL_SELIC,
        SELIC_MAIS_TAXA,
        IPCA_MAIS_TAXA,
        PREFIXADA
    }

    private record TituloSimulado(
            Remuneracao remuneracao,
            BigDecimal taxa,
            LocalDate vencimento,
            BigDecimal investimentoMinimo,
            boolean isentoIr,
            boolean resgateSoNoVencimento,
            boolean tesouro
    ) {
    }

    public SimulacaoRendimentoResponseDTO simularTituloPrivado(UUID id, SimulacaoRendimentoRequestDTO request) {
        return simularTituloPrivado(id, request, LocalDate.now());
    }

    public SimulacaoRendimentoResponseDTO simularTesouro(String codigo, SimulacaoRendimentoRequestDTO request) {
        return simularTesouro(codigo, request, LocalDate.now());
    }

    SimulacaoRendimentoResponseDTO simularTituloPrivado(UUID id, SimulacaoRendimentoRequestDTO request, LocalDate hoje) {
        TituloPrivado titulo = tituloPrivadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Título privado não encontrado"));

        Remuneracao remuneracao = switch (titulo.getIndexador()) {
            case CDI -> Remuneracao.PERCENTUAL_CDI;
            case SELIC -> Remuneracao.PERCENTUAL_SELIC;
            case IPCA -> Remuneracao.IPCA_MAIS_TAXA;
            case PREFIXADO -> Remuneracao.PREFIXADA;
        };

        return simular(new TituloSimulado(
                remuneracao,
                titulo.getTaxaPercentual(),
                titulo.getVencimento(),
                titulo.getInvestimentoMinimo(),
                titulo.isIsentoIr(),
                titulo.getLiquidez() == TipoLiquidez.NO_VENCIMENTO,
                false
        ), request, hoje);
    }

    SimulacaoRendimentoResponseDTO simularTesouro(String codigo, SimulacaoRendimentoRequestDTO request, LocalDate hoje) {
        TituloTesouro titulo = tituloTesouroRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Título do Tesouro não encontrado: " + codigo));

        Remuneracao remuneracao = switch (titulo.getTipo()) {
            case SELIC -> Remuneracao.SELIC_MAIS_TAXA;
            case IPCA -> Remuneracao.IPCA_MAIS_TAXA;
            case PREFIXADO -> Remuneracao.PREFIXADA;
        };

        return simular(new TituloSimulado(
                remuneracao,
                titulo.getTaxaAnual(),
                titulo.getVencimento(),
                titulo.getPrecoMinimo(),
                false,
                false,
                true
        ), request, hoje);
    }

    private SimulacaoRendimentoResponseDTO simular(TituloSimulado titulo, SimulacaoRendimentoRequestDTO request, LocalDate hoje) {
        IndicadoresMercado indicadores = indicadoresMercadoRepository.findTopByOrderByIdAsc()
                .filter(i -> i.getSelicAtual() != null)
                .orElseThrow(() -> new BusinessException(MENSAGEM_SEM_INDICADORES));
        if (titulo.remuneracao() == Remuneracao.IPCA_MAIS_TAXA && indicadores.getIpcaAcumulado12m() == null) {
            throw new BusinessException(MENSAGEM_SEM_INDICADORES);
        }

        BigDecimal valor = request.getValor();
        int prazoMeses = request.getPrazoMeses();
        validarValorMinimo(titulo, valor);
        LocalDate dataResgate = hoje.plusMonths(prazoMeses);
        validarPrazo(titulo, hoje, dataResgate);

        double selic = indicadores.getSelicAtual().doubleValue();
        double ipca = indicadores.getIpcaAcumulado12m() == null ? 0 : indicadores.getIpcaAcumulado12m().doubleValue();
        double taxaAnual = taxaAnualEstimada(titulo, selic, ipca);
        long prazoDias = ChronoUnit.DAYS.between(hoje, dataResgate);

        BigDecimal valorBruto = dinheiro(valor.doubleValue() * Math.pow(1 + taxaAnual, prazoMeses / 12.0));
        BigDecimal rendimentoBruto = valorBruto.subtract(valor);
        BigDecimal aliquotaIR = titulo.isentoIr() ? BigDecimal.ZERO : calculadoraRentabilidadeService.calcularAliquotaIR(prazoDias);
        BigDecimal impostoRenda = rendimentoBruto.max(BigDecimal.ZERO)
                .multiply(aliquotaIR)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal valorLiquido = valorBruto.subtract(impostoRenda);

        double poupancaMensal = poupancaMensal(selic);
        BigDecimal valorPoupanca = dinheiro(valor.doubleValue() * Math.pow(1 + poupancaMensal, prazoMeses));

        return SimulacaoRendimentoResponseDTO.builder()
                .valorInvestido(valor.setScale(2, RoundingMode.HALF_UP))
                .prazoMeses(prazoMeses)
                .prazoDias(prazoDias)
                .dataResgate(dataResgate)
                .taxaAnualEstimada(percentual(taxaAnual))
                .valorBruto(valorBruto)
                .rendimentoBruto(rendimentoBruto)
                .aliquotaIR(aliquotaIR)
                .impostoRenda(impostoRenda)
                .valorLiquido(valorLiquido)
                .rendimentoLiquido(valorLiquido.subtract(valor))
                .taxaAnualPoupanca(percentual(Math.pow(1 + poupancaMensal, 12) - 1))
                .valorPoupanca(valorPoupanca)
                .rendimentoPoupanca(valorPoupanca.subtract(valor))
                .diferencaParaPoupanca(valorLiquido.subtract(valorPoupanca))
                .premissas(premissas(titulo, selic, ipca, taxaAnual, aliquotaIR, prazoDias))
                .avisos(avisos(titulo, dataResgate))
                .indicadoresAtualizadosEm(indicadores.getSelicIpcaSincronizadoEm())
                .build();
    }

    private void validarValorMinimo(TituloSimulado titulo, BigDecimal valor) {
        if (titulo.investimentoMinimo() != null && valor.compareTo(titulo.investimentoMinimo()) < 0) {
            throw new BusinessException("O investimento mínimo deste título é R$ " + formatar(titulo.investimentoMinimo().doubleValue(), 2) + ".");
        }
    }

    private void validarPrazo(TituloSimulado titulo, LocalDate hoje, LocalDate dataResgate) {
        if (!dataResgate.isAfter(titulo.vencimento())) return;

        long prazoMaximo = ChronoUnit.MONTHS.between(hoje, titulo.vencimento());
        String vencimento = titulo.vencimento().format(DATA);
        if (prazoMaximo < 1) {
            throw new BusinessException("Este título vence em " + vencimento + ", antes de completar um mês. Não dá para simular esse prazo.");
        }
        throw new BusinessException("O prazo passa do vencimento do título, em " + vencimento + ". Escolha até " + prazoMaximo + (prazoMaximo == 1 ? " mês." : " meses."));
    }

    private double taxaAnualEstimada(TituloSimulado titulo, double selic, double ipca) {
        double taxa = titulo.taxa().doubleValue() / 100;
        return switch (titulo.remuneracao()) {
            case PERCENTUAL_CDI -> cdi(selic) / 100 * taxa;
            case PERCENTUAL_SELIC -> selic / 100 * taxa;
            case SELIC_MAIS_TAXA -> (1 + selic / 100) * (1 + taxa) - 1;
            case IPCA_MAIS_TAXA -> (1 + ipca / 100) * (1 + taxa) - 1;
            case PREFIXADA -> taxa;
        };
    }

    private double cdi(double selic) {
        return Math.max(0, selic - DIFERENCA_CDI_SELIC);
    }

    private double poupancaMensal(double selic) {
        if (selic > LIMITE_SELIC_POUPANCA) return POUPANCA_MENSAL_SELIC_ALTA;
        return Math.pow(1 + FRACAO_SELIC_POUPANCA * selic / 100, 1.0 / 12) - 1;
    }

    private List<String> premissas(TituloSimulado titulo, double selic, double ipca, double taxaAnual,
                                   BigDecimal aliquotaIR, long prazoDias) {
        List<String> premissas = new ArrayList<>();
        premissas.add("Selic atual de " + formatar(selic, 2) + "% ao ano.");
        if (titulo.remuneracao() == Remuneracao.PERCENTUAL_CDI) {
            premissas.add("CDI estimado em " + formatar(cdi(selic), 2) + "% ao ano, um pouco abaixo da Selic.");
        }
        if (titulo.remuneracao() == Remuneracao.IPCA_MAIS_TAXA) {
            premissas.add("Inflação de " + formatar(ipca, 2) + "% ao ano, igual ao IPCA dos últimos 12 meses.");
        }
        premissas.add("Rendimento do título mantido em " + formatar(taxaAnual * 100, 2) + "% ao ano durante todo o prazo.");
        if (titulo.isentoIr()) {
            premissas.add("Título isento de Imposto de Renda para pessoa física.");
        } else {
            premissas.add("Imposto de Renda de " + formatar(aliquotaIR.doubleValue(), 0, 1)
                    + "% sobre o rendimento, pela tabela regressiva para um resgate em " + prazoDias + " dias.");
        }
        premissas.add(selic > LIMITE_SELIC_POUPANCA
                ? "Poupança rendendo 0,5% ao mês, regra para quando a Selic está acima de 8,5% ao ano, sem somar a TR."
                : "Poupança rendendo 70% da Selic, regra para quando a Selic está em até 8,5% ao ano, sem somar a TR.");
        premissas.add("Não considera IOF, taxa de custódia nem outras tarifas.");
        return premissas;
    }

    private List<String> avisos(TituloSimulado titulo, LocalDate dataResgate) {
        List<String> avisos = new ArrayList<>();
        if (!dataResgate.isBefore(titulo.vencimento())) return avisos;

        if (titulo.resgateSoNoVencimento()) {
            avisos.add("Este título só pode ser resgatado no vencimento, em " + titulo.vencimento().format(DATA)
                    + ". A simulação mostra quanto ele teria rendido até a data escolhida, mas o dinheiro continua aplicado até o vencimento.");
        }
        if (titulo.tesouro() && titulo.remuneracao() != Remuneracao.SELIC_MAIS_TAXA) {
            avisos.add("Vendendo antes do vencimento, o Tesouro recompra o título pelo preço de mercado do dia, "
                    + "que pode ser maior ou menor do que o desta simulação.");
        }
        return avisos;
    }

    private static BigDecimal dinheiro(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentual(double fracao) {
        return BigDecimal.valueOf(fracao * 100).setScale(2, RoundingMode.HALF_UP);
    }

    private static String formatar(double valor, int casas) {
        return formatar(valor, casas, casas);
    }

    private static String formatar(double valor, int minimoCasas, int maximoCasas) {
        NumberFormat formato = NumberFormat.getNumberInstance(PT_BR);
        formato.setMinimumFractionDigits(minimoCasas);
        formato.setMaximumFractionDigits(maximoCasas);
        return formato.format(valor);
    }
}
