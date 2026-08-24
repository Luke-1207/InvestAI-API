package com.investai.api.infra.hgbrasil;

import com.investai.api.infra.hgbrasil.dto.HgBrasilHistoricalPointDTO;
import com.investai.api.infra.hgbrasil.dto.HgBrasilStockDTO;
import com.investai.api.infra.hgbrasil.dto.IndicadoresMercadoExternoDTO;

import java.util.List;

public interface HgBrasilClient {
    HgBrasilStockDTO obterCotacao(String ticker);
    List<HgBrasilHistoricalPointDTO> obterHistorico(String ticker, int diasAtras);
    IndicadoresMercadoExternoDTO obterIndicadoresMercado();
}