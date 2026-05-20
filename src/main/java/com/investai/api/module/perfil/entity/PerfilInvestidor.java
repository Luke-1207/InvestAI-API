package com.investai.api.module.perfil.entity;

import com.investai.api.module.auth.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "perfil_investidor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilInvestidor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "perfil_risco")
    private String perfilRisco;

    @Column(name = "horizonte")
    private String horizonte;

    @Column(name = "objetivo")
    private String objetivo;

    @Column(name = "valor_disponivel", precision = 15, scale = 2)
    private BigDecimal valorDisponivel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tipos_aceitos", columnDefinition = "jsonb")
    private List<String> tiposAceitos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "setores_preferidos", columnDefinition = "jsonb")
    private List<String> setoresPreferidos;

    @Column(name = "perfil_preenchido", nullable = false)
    private boolean perfilPreenchido = false;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
