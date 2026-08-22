CREATE TABLE indicadores_mercado (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     ibovespa_pontos DECIMAL(12,2),
                                     ibovespa_variacao_dia DECIMAL(8,4),
                                     dolar_valor DECIMAL(10,4),
                                     dolar_variacao_dia DECIMAL(8,4),
                                     euro_valor DECIMAL(10,4),
                                     euro_variacao_dia DECIMAL(8,4),
                                     selic_atual DECIMAL(8,4),
                                     ipca_acumulado_12m DECIMAL(8,4),
                                     sincronizado_em TIMESTAMP,
                                     selic_ipca_sincronizado_em TIMESTAMP
);