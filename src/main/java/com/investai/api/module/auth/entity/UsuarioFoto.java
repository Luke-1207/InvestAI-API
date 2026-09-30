package com.investai.api.module.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario_foto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioFoto {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "conteudo", nullable = false)
    private byte[] conteudo;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}