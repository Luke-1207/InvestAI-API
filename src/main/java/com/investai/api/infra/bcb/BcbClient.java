package com.investai.api.infra.bcb;

import java.math.BigDecimal;

public interface BcbClient {
    BigDecimal obterSelicAtual();
    BigDecimal obterIpcaAcumulado12Meses();
}