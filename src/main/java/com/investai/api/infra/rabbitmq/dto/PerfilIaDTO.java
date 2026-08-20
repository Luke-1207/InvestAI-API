package com.investai.api.infra.rabbitmq.dto;

import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilRisco;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PerfilIaDTO {
    private PerfilRisco perfilRisco;
    private HorizonteInvestimento horizonte;
    private ObjetivoFinanceiro objetivo;
    private BigDecimal valorDisponivel;
    private List<TipoAtivo> tiposAceitos;
    private List<SetorPreferidoIaDTO> setoresPreferidos;
}