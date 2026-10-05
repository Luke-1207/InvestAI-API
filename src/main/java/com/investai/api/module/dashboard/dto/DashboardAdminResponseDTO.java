package com.investai.api.module.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardAdminResponseDTO {
    private long totalUsuarios;
    private long novosUsuariosUltimos30Dias;
    private long usuariosComPerfilPreenchido;
    private Map<String, Long> distribuicaoRisco;
    private Map<String, Long> distribuicaoAtivosPorCategoria;
    private long titulosVencendoEm30Dias;
    private List<TituloVencendoDTO> titulosVencendo;
    private LocalDateTime ultimaSincronizacaoTesouro;
    private boolean iaDisponivel;
    private Boolean iaRabbitmqConectado;
    private LocalDateTime geradoEm;
}