package com.investai.api.module.ativo.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CotacaoResponseDTO {

    private String codigo;
    private String nome;
    private String setor;
    private BigDecimal preco;
    private BigDecimal variacaoPercentual;
    private BigDecimal variacaoPreco;
    private BigDecimal dividendYield;
    private BigDecimal precoValorPatrimonial;
    private Long volume;
    private LocalDateTime atualizadoEm;
    private String fonte;
}
