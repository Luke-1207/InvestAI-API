package com.investai.api.module.ativo.dto;

import com.investai.api.module.ativo.entity.TipoAtivo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AcaoResponseDTO {

    private UUID id;
    private String codigo;
    private String nome;
    private TipoAtivo tipo;
    private String setor;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}