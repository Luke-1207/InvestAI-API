package com.investai.api.infra.tesourodireto;

import com.investai.api.infra.tesourodireto.dto.TituloTesouroExternoDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Títulos sintéticos fixos pra dar volume ao catálogo, já que só 3 títulos
 * reais estão disponíveis sem plano pago na brapi.dev. Reaproveitado tanto
 * pelo client real (junto com os 3 reais) quanto pelo client 100% mock.
 */
final class TesouroDiretoFixturesVolume {

    private TesouroDiretoFixturesVolume() {
    }

    static List<TituloTesouroExternoDTO> gerarTitulosDeVolume() {
        return List.of(
                TituloTesouroExternoDTO.builder()
                        .codigo("tesouro-selic-2029-mock")
                        .nome("Tesouro Selic 2029")
                        .tipo("SELIC")
                        .taxaAnual(BigDecimal.valueOf(0.05))
                        .precoMinimo(BigDecimal.valueOf(150.00))
                        .vencimento(LocalDate.of(2029, 3, 1))
                        .pagaJurosSemestrais(false)
                        .build(),
                TituloTesouroExternoDTO.builder()
                        .codigo("tesouro-ipca-2035-mock")
                        .nome("Tesouro IPCA+ 2035")
                        .tipo("IPCA")
                        .taxaAnual(BigDecimal.valueOf(6.24))
                        .precoMinimo(BigDecimal.valueOf(45.00))
                        .vencimento(LocalDate.of(2035, 5, 15))
                        .pagaJurosSemestrais(false)
                        .build(),
                TituloTesouroExternoDTO.builder()
                        .codigo("tesouro-prefixado-2027-mock")
                        .nome("Tesouro Prefixado 2027")
                        .tipo("PREFIXADO")
                        .taxaAnual(BigDecimal.valueOf(12.87))
                        .precoMinimo(BigDecimal.valueOf(760.00))
                        .vencimento(LocalDate.of(2027, 1, 1))
                        .pagaJurosSemestrais(false)
                        .build(),
                TituloTesouroExternoDTO.builder()
                        .codigo("tesouro-ipca-2029-mock")
                        .nome("Tesouro IPCA+ 2029")
                        .tipo("IPCA")
                        .taxaAnual(BigDecimal.valueOf(5.98))
                        .precoMinimo(BigDecimal.valueOf(90.00))
                        .vencimento(LocalDate.of(2029, 8, 15))
                        .pagaJurosSemestrais(true)
                        .build()
        );
    }
}