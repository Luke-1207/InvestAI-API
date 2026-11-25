package com.investai.api.module.favorito.entity;

import com.investai.api.module.notificacao.entity.TipoNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "favorito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorito {

    public static final BigDecimal LIMIAR_VARIACAO_PADRAO = new BigDecimal("5.00");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "ativo_id", nullable = false)
    private UUID ativoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ativo", nullable = false, length = 20)
    private TipoAtivoFavorito tipoAtivo;

    @Builder.Default
    @Column(name = "alertas_ativos", nullable = false)
    private boolean alertasAtivos = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tipos_alerta_ativos", columnDefinition = "jsonb")
    private List<TipoNotificacao> tiposAlertaAtivos;

    @Builder.Default
    @Column(name = "limiar_variacao", nullable = false, precision = 5, scale = 2)
    private BigDecimal limiarVariacao = LIMIAR_VARIACAO_PADRAO;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public boolean alertaHabilitado(TipoNotificacao tipo) {
        return alertasAtivos && (tiposAlertaAtivos == null || tiposAlertaAtivos.contains(tipo));
    }
}
