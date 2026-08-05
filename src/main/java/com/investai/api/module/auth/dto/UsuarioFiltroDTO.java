package com.investai.api.module.auth.dto;

import com.investai.api.module.auth.entity.Role;
import lombok.Data;

@Data
public class UsuarioFiltroDTO {
    private String busca;
    private Role role;
    private Boolean ativo;
}
