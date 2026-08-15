package com.investai.api.module.perfil.dto;

import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.entity.SetorPreferido;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PerfilResponseDTO {
    private ValorDescritoDTO perfilRisco;
    private ValorDescritoDTO objetivoFinanceiro;
    private ValorDescritoDTO horizonteInvestimento;
    private BigDecimal valorDisponivel;
    private List<TipoAtivo> tiposAceitos;
    private List<SetorPreferido> setoresPreferidos;
    private boolean perfilPreenchido;
    private String resumoIA;
    private LocalDateTime atualizadoEm;
}