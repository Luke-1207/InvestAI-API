package com.investai.api.module.perfil.dto;

import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilRisco;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EditarPerfilRequestDTO {

    @NotNull(message = "perfilRisco é obrigatório")
    private PerfilRisco perfilRisco;

    @NotNull(message = "horizonteInvestimento é obrigatório")
    private HorizonteInvestimento horizonteInvestimento;

    @NotNull(message = "objetivoFinanceiro é obrigatório")
    private ObjetivoFinanceiro objetivoFinanceiro;

    @NotNull(message = "valorDisponivel é obrigatório")
    @DecimalMin(value = "0.0", message = "valorDisponivel não pode ser negativo")
    private BigDecimal valorDisponivel;

    @NotEmpty(message = "Selecione ao menos um tipo de ativo aceito")
    private List<TipoAtivo> tiposAceitos;

    @NotNull(message = "setoresPreferidos é obrigatório (pode ser lista vazia)")
    @Valid
    private List<SetorPreferidoRequestDTO> setoresPreferidos;
}