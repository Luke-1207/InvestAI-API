package com.investai.api.module.auth.dto;

import com.investai.api.module.auth.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CadastroResponseDTO {

    private UUID id;
    private String nome;
    private String email;
    private Role role;

}
