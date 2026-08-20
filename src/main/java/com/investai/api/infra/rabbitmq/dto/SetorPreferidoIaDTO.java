package com.investai.api.infra.rabbitmq.dto;

import com.investai.api.module.perfil.entity.PreferenciaSetor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetorPreferidoIaDTO {
    private String setor;
    private PreferenciaSetor preferencia;
}