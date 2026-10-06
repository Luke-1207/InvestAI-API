package com.investai.api.module.relatorio.dto;

import com.investai.api.module.relatorio.entity.TipoRelatorio;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class HistoricoRelatorioResponseDTO {
    private UUID id;
    private TipoRelatorio tipo;
    private String referencia;
    private LocalDateTime geradoEm;
}
