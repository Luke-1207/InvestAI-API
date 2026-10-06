package com.investai.api.module.relatorio.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.relatorio.dto.RentabilidadePrazoDTO;
import com.investai.api.module.relatorio.dto.TituloRendaFixaRelatorio;
import com.investai.api.module.rendafixa.dto.RentabilidadeEstimadaDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import com.investai.api.module.rendafixa.service.CalculadoraRentabilidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RelatorioRendaFixaService {

    public static final String GARANTIA_FGC = "Garantido pelo FGC";
    public static final String SEM_GARANTIA_FGC = "Sem garantia do FGC";
    public static final String GARANTIA_TESOURO = "Garantido pelo Tesouro Nacional";
    public static final String LIQUIDEZ_DIARIA = "Diária";
    public static final String LIQUIDEZ_NO_VENCIMENTO = "No vencimento";
    public static final String CATEGORIA_TESOURO = "Tesouro Direto";
    public static final String PRAZO_VENCIMENTO = "No vencimento";

    private static final List<Long> PRAZOS_EM_DIAS = List.of(180L, 360L, 720L);

    private final TituloPrivadoRepository tituloPrivadoRepository;
    private final TituloTesouroRepository tituloTesouroRepository;
    private final CalculadoraRentabilidadeService calculadoraRentabilidadeService;

    public TituloRendaFixaRelatorio buscarTitulo(String identificador) {
        String valor = identificador.trim();
        UUID id = comoUuid(valor);
        if (id != null) {
            return tituloPrivadoRepository.findById(id)
                    .map(this::dePrivado)
                    .orElseThrow(() -> new ResourceNotFoundException("Título privado não encontrado"));
        }
        return tituloTesouroRepository.findByCodigo(valor)
                .map(this::deTesouro)
                .orElseThrow(() -> new ResourceNotFoundException("Título do Tesouro não encontrado: " + valor));
    }

    private TituloRendaFixaRelatorio dePrivado(TituloPrivado titulo) {
        return TituloRendaFixaRelatorio.builder()
                .identificador(titulo.getTipo().name() + "-" + titulo.getEmissor())
                .nome(titulo.getTipo().name() + " " + titulo.getEmissor())
                .categoria(titulo.getTipo().name())
                .indexador(titulo.getIndexador())
                .taxa(titulo.getTaxaPercentual())
                .taxaSomadaAoIndexador(titulo.getIndexador() == Indexador.IPCA)
                .vencimento(titulo.getVencimento())
                .investimentoMinimo(titulo.getInvestimentoMinimo())
                .liquidez(titulo.getLiquidez() == TipoLiquidez.DIARIA ? LIQUIDEZ_DIARIA : LIQUIDEZ_NO_VENCIMENTO)
                .garantia(titulo.isGarantidoFgc() ? GARANTIA_FGC : SEM_GARANTIA_FGC)
                .isentoIr(titulo.isIsentoIr())
                .rentabilidades(calcularRentabilidades(
                        titulo.getTaxaPercentual(), titulo.isIsentoIr(), titulo.getVencimento(),
                        titulo.getIndexador() == Indexador.IPCA))
                .build();
    }

    private TituloRendaFixaRelatorio deTesouro(TituloTesouro titulo) {
        return TituloRendaFixaRelatorio.builder()
                .identificador(titulo.getCodigo())
                .nome(titulo.getNome())
                .categoria(CATEGORIA_TESOURO)
                .indexador(Indexador.valueOf(titulo.getTipo().name()))
                .taxa(titulo.getTaxaAnual())
                .taxaSomadaAoIndexador(titulo.getTipo() != TipoTesouro.PREFIXADO)
                .vencimento(titulo.getVencimento())
                .investimentoMinimo(titulo.getPrecoMinimo())
                .liquidez(LIQUIDEZ_DIARIA)
                .garantia(GARANTIA_TESOURO)
                .isentoIr(false)
                .rentabilidades(calcularRentabilidades(titulo.getTaxaAnual(), false, titulo.getVencimento(),
                        titulo.getTipo() != TipoTesouro.PREFIXADO))
                .build();
    }

    private List<RentabilidadePrazoDTO> calcularRentabilidades(
            BigDecimal taxa, boolean isentoIr, LocalDate vencimento, boolean taxaSomadaAoIndexador
    ) {
        long diasAteVencimento = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), vencimento));

        List<RentabilidadePrazoDTO> linhas = new ArrayList<>();
        for (long prazo : PRAZOS_EM_DIAS) {
            if (prazo < diasAteVencimento) {
                linhas.add(linha(prazo + " dias", taxa, isentoIr, prazo, taxaSomadaAoIndexador));
            }
        }
        linhas.add(linha(PRAZO_VENCIMENTO + " (" + diasAteVencimento + " dias)", taxa, isentoIr, diasAteVencimento,
                taxaSomadaAoIndexador));
        return linhas;
    }

    private RentabilidadePrazoDTO linha(
            String prazo, BigDecimal taxa, boolean isentoIr, long dias, boolean taxaSomadaAoIndexador
    ) {
        RentabilidadeEstimadaDTO estimativa = calculadoraRentabilidadeService.calcularParaPrazo(taxa, isentoIr, dias);
        boolean liquidaNaMesmaUnidade = isentoIr || !taxaSomadaAoIndexador;
        return new RentabilidadePrazoDTO(
                prazo,
                estimativa.getAliquotaIR(),
                estimativa.getTaxaBrutaAnual(),
                liquidaNaMesmaUnidade ? estimativa.getTaxaLiquidaAnual() : null);
    }

    private UUID comoUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
