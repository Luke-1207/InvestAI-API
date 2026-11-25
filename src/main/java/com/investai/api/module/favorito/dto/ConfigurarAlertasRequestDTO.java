package com.investai.api.module.favorito.dto;

import com.investai.api.module.notificacao.entity.TipoNotificacao;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurarAlertasRequestDTO {

    @NotNull(message = "Informe se os alertas estão ativos")
    private Boolean alertasAtivos;

    private List<TipoNotificacao> tiposAlertaAtivos;

    @DecimalMin(value = "0.01", message = "O limiar de variação deve ser maior que zero")
    @DecimalMax(value = "100.00", message = "O limiar de variação deve ser no máximo 100%")
    @Digits(integer = 3, fraction = 2, message = "O limiar de variação aceita até 2 casas decimais")
    private BigDecimal limiarVariacao;
}
