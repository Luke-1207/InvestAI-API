package com.investai.api.infra.rabbitmq.dto;

import com.investai.api.module.ativo.entity.TipoAtivo;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AtivoRankingDTO {
    private String codigo;
    private TipoAtivo tipo;
    private String setor;
    private BigDecimal preco;
    private BigDecimal dy;
    private BigDecimal variacao30d;
    private Volatilidade volatilidade;
}