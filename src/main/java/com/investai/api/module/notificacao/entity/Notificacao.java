package com.investai.api.module.notificacao.entity;

import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacao {

    public static final int TAMANHO_MAXIMO_TITULO = 150;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacao tipo;

    @Column(nullable = false, length = TAMANHO_MAXIMO_TITULO)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String mensagem;

    @Column(name = "ativo_id")
    private UUID ativoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ativo", length = 20)
    private TipoAtivoFavorito tipoAtivo;

    @Builder.Default
    @Column(nullable = false)
    private boolean lida = false;

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public void marcarComoLida(LocalDateTime agora) {
        if (lida) {
            return;
        }
        lida = true;
        lidaEm = agora;
    }
}
