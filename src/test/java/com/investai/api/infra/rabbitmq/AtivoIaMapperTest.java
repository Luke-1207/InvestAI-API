package com.investai.api.infra.rabbitmq;

import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AtivoIaMapperTest {

    private AcaoDetalheResponseDTO.AcaoDetalheResponseDTOBuilder acao() {
        return AcaoDetalheResponseDTO.builder()
                .codigo("TAEE11")
                .nome("Transmissora Aliança")
                .tipo(TipoAtivo.ACAO)
                .setor("Energia Elétrica")
                .preco(new BigDecimal("38.42"))
                .variacaoPercentual(new BigDecimal("1.23"))
                .dividendYield(new BigDecimal("8.4"));
    }

    @Test
    @DisplayName("deAcao - deve montar o ativo de renda variável com os campos do contrato da IA")
    void deAcao_deveMontarCamposDoContrato() {
        Map<String, Object> mapa = AtivoIaMapper.deAcao(acao()
                .precoLucro(new BigDecimal("9.1"))
                .precoValorPatrimonial(new BigDecimal("1.35"))
                .minimo52Semanas(new BigDecimal("31.2"))
                .maximo52Semanas(new BigDecimal("41.9"))
                .build());

        assertThat(mapa)
                .containsEntry("codigo", "TAEE11")
                .containsEntry("nome", "Transmissora Aliança")
                .containsEntry("tipo", "ACAO")
                .containsEntry("setor", "Energia Elétrica")
                .containsEntry("preco", new BigDecimal("38.42"))
                .containsEntry("dy", new BigDecimal("8.4"))
                .containsEntry("variacao30d", new BigDecimal("1.23"))
                .containsEntry("pl", new BigDecimal("9.1"))
                .containsEntry("pvp", new BigDecimal("1.35"))
                .containsEntry("variacao52s",
                        Map.of("min", new BigDecimal("31.2"), "max", new BigDecimal("41.9")));
    }

    @Test
    @DisplayName("deAcao - deve omitir indicadores ausentes e zerar dividend yield e variação nulos")
    void deAcao_deveOmitirOpcionaisEZerarNulos() {
        Map<String, Object> mapa = AtivoIaMapper.deAcao(acao()
                .dividendYield(null)
                .variacaoPercentual(null)
                .precoValorPatrimonial(BigDecimal.ZERO)
                .minimo52Semanas(new BigDecimal("31.2"))
                .build());

        assertThat(mapa)
                .containsEntry("dy", BigDecimal.ZERO)
                .containsEntry("variacao30d", BigDecimal.ZERO)
                .doesNotContainKeys("pl", "pvp", "variacao52s");
    }

    @Test
    @DisplayName("deTesouro - deve montar o título público com liquidez diária e sem FGC")
    void deTesouro_deveMontarTituloPublico() {
        TituloTesouro titulo = TituloTesouro.builder()
                .id(UUID.randomUUID())
                .codigo("tesouro-ipca-2035")
                .nome("Tesouro IPCA+ 2035")
                .tipo(TipoTesouro.IPCA)
                .taxaAnual(new BigDecimal("6.15"))
                .precoMinimo(new BigDecimal("42.30"))
                .vencimento(LocalDate.of(2035, 5, 15))
                .build();

        assertThat(AtivoIaMapper.deTesouro(titulo))
                .containsEntry("codigo", "tesouro-ipca-2035")
                .containsEntry("nome", "Tesouro IPCA+ 2035")
                .containsEntry("tipo", "TESOURO")
                .containsEntry("indexador", "IPCA")
                .containsEntry("taxaPercentual", new BigDecimal("6.15"))
                .containsEntry("vencimento", "2035-05-15")
                .containsEntry("investimentoMinimo", new BigDecimal("42.30"))
                .containsEntry("liquidez", "DIARIA")
                .containsEntry("isentoIR", false)
                .containsEntry("garantidoFGC", false);
    }

    @Test
    @DisplayName("deTituloPrivado - deve usar o id como código e levar isenção de IR e FGC")
    void deTituloPrivado_deveMontarTituloPrivado() {
        UUID id = UUID.randomUUID();
        TituloPrivado titulo = TituloPrivado.builder()
                .id(id)
                .tipo(TipoTituloPrivado.LCI)
                .emissor("Banco Inter")
                .indexador(Indexador.CDI)
                .taxaPercentual(new BigDecimal("92"))
                .vencimento(LocalDate.of(2028, 3, 1))
                .investimentoMinimo(new BigDecimal("1000"))
                .liquidez(TipoLiquidez.NO_VENCIMENTO)
                .garantidoFgc(true)
                .isentoIr(true)
                .build();

        assertThat(AtivoIaMapper.deTituloPrivado(titulo))
                .containsEntry("codigo", id.toString())
                .containsEntry("nome", "LCI - Banco Inter")
                .containsEntry("emissor", "Banco Inter")
                .containsEntry("tipo", "LCI")
                .containsEntry("indexador", "CDI")
                .containsEntry("taxaPercentual", new BigDecimal("92"))
                .containsEntry("vencimento", "2028-03-01")
                .containsEntry("investimentoMinimo", new BigDecimal("1000"))
                .containsEntry("liquidez", "NO_VENCIMENTO")
                .containsEntry("isentoIR", true)
                .containsEntry("garantidoFGC", true);
    }
}
