package com.investai.api.module.auth.dto;

import com.investai.api.module.auth.entity.Role;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CadastroResponseDTO {

    private UUID id;
    private String nome;
    private String email;
    private Role role;

}
