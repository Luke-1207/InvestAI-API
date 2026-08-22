package com.investai.api.module.dashboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "indicadores_mercado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicadoresMercado {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ibovespa_pontos", precision = 12, scale = 2)
    private BigDecimal ibovespaPontos;

    @Column(name = "ibovespa_variacao_dia", precision = 8, scale = 4)
    private BigDecimal ibovespaVariacaoDia;

    @Column(name = "dolar_valor", precision = 10, scale = 4)
    private BigDecimal dolarValor;

    @Column(name = "dolar_variacao_dia", precision = 8, scale = 4)
    private BigDecimal dolarVariacaoDia;

    @Column(name = "euro_valor", precision = 10, scale = 4)
    private BigDecimal euroValor;

    @Column(name = "euro_variacao_dia", precision = 8, scale = 4)
    private BigDecimal euroVariacaoDia;

    @Column(name = "selic_atual", precision = 8, scale = 4)
    private BigDecimal selicAtual;

    @Column(name = "ipca_acumulado_12m", precision = 8, scale = 4)
    private BigDecimal ipcaAcumulado12m;

    @Column(name = "sincronizado_em")
    private LocalDateTime sincronizadoEm;

    @Column(name = "selic_ipca_sincronizado_em")
    private LocalDateTime selicIpcaSincronizadoEm;
}