package com.investai.api.module.rendafixa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "titulo_privado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TituloPrivado {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTituloPrivado tipo;

    @Column(nullable = false, length = 150)
    private String emissor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Indexador indexador;

    @Column(name = "taxa_percentual", nullable = false, precision = 8, scale = 4)
    private BigDecimal taxaPercentual;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(name = "investimento_minimo", nullable = false, precision = 15, scale = 2)
    private BigDecimal investimentoMinimo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLiquidez liquidez;

    @Column(name = "garantido_fgc", nullable = false)
    private boolean garantidoFgc;

    @Column(name = "isento_ir", nullable = false)
    private boolean isentoIr;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}