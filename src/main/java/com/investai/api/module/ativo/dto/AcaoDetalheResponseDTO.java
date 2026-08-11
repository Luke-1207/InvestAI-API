package com.investai.api.module.ativo.dto;

import com.investai.api.module.ativo.entity.TipoAtivo;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AcaoDetalheResponseDTO {

    private UUID id;
    private String codigo;
    private String nome;
    private TipoAtivo tipo;
    private String setor;
    private boolean ativo;

    private boolean cotacaoDisponivel;
    private BigDecimal preco;
    private BigDecimal variacaoPercentual;
    private BigDecimal variacaoPreco;
    private BigDecimal dividendYield;
    private BigDecimal precoValorPatrimonial;
    private BigDecimal precoLucro;
    private Long volume;
    private LocalDateTime cotacaoAtualizadaEm;
    private String fonteCotacao;

    private BigDecimal minimo52Semanas;
    private BigDecimal maximo52Semanas;

    private String periodoGrafico;
    private List<PontoHistoricoDTO> pontosGrafico;

    private Map<String, String> glossario;
}