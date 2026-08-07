package com.investai.api.infra.hgbrasil;

import com.investai.api.infra.hgbrasil.dto.HgBrasilStockDTO;

public interface HgBrasilClient {
    HgBrasilStockDTO obterCotacao(String ticker);
}