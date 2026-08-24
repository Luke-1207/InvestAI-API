package com.investai.api.module.dashboard.dto;

import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DashboardResponseDTO {
    private IndicadoresMercadoResponseDTO indicadoresMercado;
    private SugestoesRendaVariavelResponseDTO sugestoesRendaVariavel;
    private SugestoesRendaFixaResponseDTO sugestoesRendaFixa;
    private PerfilResponseDTO perfil;
    private LocalDateTime geradoEm;
}