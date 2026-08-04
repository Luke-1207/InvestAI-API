package com.investai.api.module.auth.dto;

import com.investai.api.module.auth.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UsuarioDetalheResponseDTO {
    private UUID id;
    private String nome;
    private String email;
    private Role role;
    private boolean ativo;
    private boolean perfilPreenchido;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime deletadoEm;
}
