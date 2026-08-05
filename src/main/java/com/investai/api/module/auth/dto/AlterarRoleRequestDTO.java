package com.investai.api.module.auth.dto;

import com.investai.api.module.auth.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlterarRoleRequestDTO {
    @NotNull(message = "Role é obrigatória")
    private Role role;
}
