package com.investai.api.infra.rabbitmq;

import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class AtivoIaMapper {

    private static final String TIPO_TESOURO = "TESOURO";

    private AtivoIaMapper() {
    }

    public static Map<String, Object> deAcao(AcaoDetalheResponseDTO ativo) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("codigo", ativo.getCodigo());
        mapa.put("nome", ativo.getNome());
        mapa.put("tipo", ativo.getTipo().name());
        mapa.put("setor", ativo.getSetor());
        mapa.put("preco", ativo.getPreco());
        mapa.put("dy", naoNegativo(ativo.getDividendYield()));
        // TODO: trocar pela variação real de 30 dias quando a cotação passar a trazer esse dado
        mapa.put("variacao30d", ativo.getVariacaoPercentual() != null ? ativo.getVariacaoPercentual() : BigDecimal.ZERO);
        if (positivo(ativo.getPrecoLucro())) {
            mapa.put("pl", ativo.getPrecoLucro());
        }
        if (positivo(ativo.getPrecoValorPatrimonial())) {
            mapa.put("pvp", ativo.getPrecoValorPatrimonial());
        }
        if (ativo.getMinimo52Semanas() != null && ativo.getMaximo52Semanas() != null) {
            mapa.put("variacao52s", Map.of("min", ativo.getMinimo52Semanas(), "max", ativo.getMaximo52Semanas()));
        }
        return mapa;
    }

    public static Map<String, Object> deTesouro(TituloTesouro titulo) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("codigo", titulo.getCodigo());
        mapa.put("nome", titulo.getNome());
        mapa.put("emissor", "Tesouro Nacional");
        mapa.put("tipo", TIPO_TESOURO);
        mapa.put("indexador", titulo.getTipo().name());
        mapa.put("taxaPercentual", titulo.getTaxaAnual());
        mapa.put("vencimento", titulo.getVencimento().toString());
        mapa.put("investimentoMinimo", naoNegativo(titulo.getPrecoMinimo()));
        mapa.put("liquidez", TipoLiquidez.DIARIA.name());
        mapa.put("isentoIR", false);
        mapa.put("garantidoFGC", false);
        return mapa;
    }

    public static Map<String, Object> deTituloPrivado(TituloPrivado titulo) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("codigo", titulo.getId().toString());
        mapa.put("nome", titulo.getTipo().name() + " - " + titulo.getEmissor());
        mapa.put("emissor", titulo.getEmissor());
        mapa.put("tipo", titulo.getTipo().name());
        mapa.put("indexador", titulo.getIndexador().name());
        mapa.put("taxaPercentual", titulo.getTaxaPercentual());
        mapa.put("vencimento", titulo.getVencimento().toString());
        mapa.put("investimentoMinimo", naoNegativo(titulo.getInvestimentoMinimo()));
        mapa.put("liquidez", titulo.getLiquidez().name());
        mapa.put("isentoIR", titulo.isIsentoIr());
        mapa.put("garantidoFGC", titulo.isGarantidoFgc());
        return mapa;
    }

    private static BigDecimal naoNegativo(BigDecimal valor) {
        return valor == null || valor.signum() < 0 ? BigDecimal.ZERO : valor;
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
