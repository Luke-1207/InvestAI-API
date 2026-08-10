package com.investai.api.module.ativo.dto;

import com.investai.api.module.ativo.entity.TipoAtivo;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AcaoListagemResponseDTO {
    private UUID id;
    private String codigo;
    private String nome;
    private TipoAtivo tipo;
    private String setor;
    private BigDecimal preco;
    private BigDecimal variacaoPercentual;
    private BigDecimal dividendYield;
    private BigDecimal precoValorPatrimonial;
    private Long volume;
    private boolean cotacaoDisponivel;
}
