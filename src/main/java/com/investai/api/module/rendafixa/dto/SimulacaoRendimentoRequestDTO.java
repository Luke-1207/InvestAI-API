package com.investai.api.module.rendafixa.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulacaoRendimentoRequestDTO {

    @NotNull(message = "Informe o valor que você quer investir.")
    @DecimalMin(value = "1.00", message = "O valor precisa ser de pelo menos R$ 1,00.")
    @DecimalMax(value = "100000000.00", message = "O valor pode ser de no máximo R$ 100.000.000,00.")
    private BigDecimal valor;

    @NotNull(message = "Informe o prazo em meses.")
    @Min(value = 1, message = "O prazo precisa ser de pelo menos 1 mês.")
    @Max(value = 600, message = "O prazo pode ser de no máximo 600 meses.")
    private Integer prazoMeses;
}
