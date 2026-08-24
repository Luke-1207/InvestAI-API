package com.investai.api.module.dashboard.dto;

import com.investai.api.infra.rabbitmq.dto.Compatibilidade;
import com.investai.api.module.ativo.entity.TipoAtivo;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SugestaoAtivoItemDTO {
    private String codigo;
    private String nome;
    private TipoAtivo tipo;
    private String setor;
    private BigDecimal preco;
    private BigDecimal variacaoDia;
    private BigDecimal dy;
    private Integer score;
    private Compatibilidade compatibilidade;
    private String justificativa;
}