package com.investai.api.module.dashboard.dto;

import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TituloVencendoDTO {
    private UUID id;
    private TipoTituloPrivado tipo;
    private String emissor;
    private LocalDate vencimento;
    private long diasParaVencimento;
    private NivelUrgencia urgencia;
}
