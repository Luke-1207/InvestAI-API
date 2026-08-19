package com.investai.api.infra.tesourodireto;

import com.investai.api.infra.exception.TesouroDiretoIndisponivelException;
import com.investai.api.infra.tesourodireto.dto.BrapiTreasuryDTO;
import com.investai.api.infra.tesourodireto.dto.BrapiTreasuryIndicatorsResponseDTO;
import com.investai.api.infra.tesourodireto.dto.TituloTesouroExternoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A API "não-oficial" do Tesouro Direto (tesourodireto.com.br/json/...) está
 * bloqueada por Cloudflare pra chamadas de servidor (403 em qualquer request
 * programática) — nunca foi uma API oficial, era um endpoint interno do site
 * "emprestado" pela comunidade dev, sem suporte garantido.
 * <p>
 * Usamos a brapi.dev (brapi.dev/docs/tesouro-direto) como fonte real no lugar
 * dela — REST de verdade, sem bloqueio. Sem assinar o plano Pro, só 3 títulos
 * fixos funcionam sem token (sandbox deles). Por isso buscamos esses 3 de
 * verdade e completamos o catálogo com títulos sintéticos fixos só pra dar
 * volume de demonstração — decisão consciente e documentada, mesmo espírito
 * do P/L sempre nulo da HG Brasil.
 */
@Service
@ConditionalOnProperty(name = "tesourodireto.mock-enabled", havingValue = "false")
@RequiredArgsConstructor
@Slf4j
public class TesouroDiretoBrapiClient implements TesouroDiretoClient {

    private static final String SIMBOLOS_SANDBOX_GRATUITOS =
            "tesouro-selic-01032031,tesouro-prefixado-com-juros-semestrais-01012037,tesouro-ipca-com-juros-semestrais-15082060";

    private static final Map<String, String> INDEXADOR_PARA_TIPO = Map.of(
            "selic", "SELIC",
            "prefixado", "PREFIXADO",
            "ipca", "IPCA"
    );

    private final RestClient tesouroDiretoRestClient;

    @Value("${tesourodireto.base-url}")
    private String baseUrl;

    @Override
    public List<TituloTesouroExternoDTO> buscarTitulosDisponiveis() {
        List<TituloTesouroExternoDTO> titulos = new ArrayList<>(buscarTitulosReaisNaBrapi());
        titulos.addAll(TesouroDiretoFixturesVolume.gerarTitulosDeVolume());
        return titulos;
    }

    private List<TituloTesouroExternoDTO> buscarTitulosReaisNaBrapi() {
        String url = String.format("%s/indicators?symbols=%s", baseUrl, SIMBOLOS_SANDBOX_GRATUITOS);

        try {
            BrapiTreasuryIndicatorsResponseDTO response = tesouroDiretoRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(BrapiTreasuryIndicatorsResponseDTO.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                throw new TesouroDiretoIndisponivelException("Resposta vazia da brapi.dev");
            }

            return response.getResults().stream()
                    .map(this::toExternoDTO)
                    .toList();

        } catch (RestClientException e) {
            log.error("Falha ao consultar brapi.dev para o Tesouro Direto: {}", e.getMessage());
            throw new TesouroDiretoIndisponivelException("Serviço de Tesouro Direto indisponível no momento");
        }
    }

    private TituloTesouroExternoDTO toExternoDTO(BrapiTreasuryDTO dto) {
        LocalDate vencimento = LocalDate.parse(dto.getMaturityDate());
        String tipo = INDEXADOR_PARA_TIPO.getOrDefault(dto.getIndexer(), "PREFIXADO");

        return TituloTesouroExternoDTO.builder()
                .codigo(dto.getSymbol())
                .nome(dto.getBondType() + " " + vencimento.getYear())
                .tipo(tipo)
                .taxaAnual(BigDecimal.valueOf(dto.getBuyRate()))
                .precoMinimo(BigDecimal.valueOf(dto.getBuyPrice())
                        .multiply(BigDecimal.valueOf(0.01))
                        .setScale(2, RoundingMode.HALF_UP))
                .vencimento(vencimento)
                .pagaJurosSemestrais("semestral".equals(dto.getCouponType()))
                .build();
    }
}