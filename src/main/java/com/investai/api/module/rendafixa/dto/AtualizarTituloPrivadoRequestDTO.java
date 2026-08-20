package com.investai.api.module.rendafixa.dto;

import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AtualizarTituloPrivadoRequestDTO {

    @NotNull(message = "Tipo é obrigatório")
    private TipoTituloPrivado tipo;

    @NotBlank(message = "Emissor é obrigatório")
    @Size(max = 150, message = "Emissor deve ter no máximo 150 caracteres")
    private String emissor;

    @NotNull(message = "Indexador é obrigatório")
    private Indexador indexador;

    @NotNull(message = "Taxa percentual é obrigatória")
    @Positive(message = "Taxa percentual deve ser positiva")
    private BigDecimal taxaPercentual;

    @NotNull(message = "Vencimento é obrigatório")
    @Future(message = "Vencimento deve ser uma data futura")
    private LocalDate vencimento;

    @NotNull(message = "Investimento mínimo é obrigatório")
    @Positive(message = "Investimento mínimo deve ser positivo")
    private BigDecimal investimentoMinimo;

    @NotNull(message = "Liquidez é obrigatória")
    private TipoLiquidez liquidez;

    @NotNull(message = "Informe se é garantido pelo FGC")
    private Boolean garantidoFgc;

    @NotNull(message = "Informe se é isento de IR")
    private Boolean isentoIr;
}