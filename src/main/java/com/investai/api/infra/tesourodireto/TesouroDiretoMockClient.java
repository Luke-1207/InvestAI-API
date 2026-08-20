package com.investai.api.infra.tesourodireto;

import com.investai.api.infra.tesourodireto.dto.TituloTesouroExternoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 100% offline — nenhuma chamada de rede, nem pra brapi.dev. Útil pra
 * desenvolvimento local sem internet ou quando não se quer depender de
 * nenhum serviço externo.
 */
@Service
@ConditionalOnProperty(name = "tesourodireto.mock-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class TesouroDiretoMockClient implements TesouroDiretoClient {

    @Override
    public List<TituloTesouroExternoDTO> buscarTitulosDisponiveis() {
        log.debug("[MOCK] Retornando catálogo sintético de títulos do Tesouro Direto");

        List<TituloTesouroExternoDTO> titulos = new ArrayList<>();
        titulos.add(TituloTesouroExternoDTO.builder()
                .codigo("tesouro-selic-2031-mock")
                .nome("Tesouro Selic 2031")
                .tipo("SELIC")
                .taxaAnual(BigDecimal.valueOf(0.08))
                .precoMinimo(BigDecimal.valueOf(189.44))
                .vencimento(LocalDate.of(2031, 3, 1))
                .pagaJurosSemestrais(false)
                .build());
        titulos.add(TituloTesouroExternoDTO.builder()
                .codigo("tesouro-prefixado-semestral-2037-mock")
                .nome("Tesouro Prefixado com Juros Semestrais 2037")
                .tipo("PREFIXADO")
                .taxaAnual(BigDecimal.valueOf(14.36))
                .precoMinimo(BigDecimal.valueOf(8.15))
                .vencimento(LocalDate.of(2037, 1, 1))
                .pagaJurosSemestrais(true)
                .build());
        titulos.add(TituloTesouroExternoDTO.builder()
                .codigo("tesouro-ipca-semestral-2060-mock")
                .nome("Tesouro IPCA+ com Juros Semestrais 2060")
                .tipo("IPCA")
                .taxaAnual(BigDecimal.valueOf(7.22))
                .precoMinimo(BigDecimal.valueOf(40.87))
                .vencimento(LocalDate.of(2060, 8, 15))
                .pagaJurosSemestrais(true)
                .build());
        titulos.addAll(TesouroDiretoFixturesVolume.gerarTitulosDeVolume());
        return titulos;
    }
}