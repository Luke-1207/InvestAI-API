package com.investai.api.module.rendafixa.dto;

import com.investai.api.module.rendafixa.entity.TipoTesouro;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TituloTesouroListagemResponseDTO {
    private UUID id;
    private String codigo;
    private String nome;
    private TipoTesouro tipo;
    private BigDecimal taxaAnual;
    private BigDecimal precoMinimo;
    private LocalDate vencimento;
    private boolean pagaJurosSemestrais;
}