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
@Table(name = "titulo_tesouro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TituloTesouro {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoTesouro tipo;

    @Column(name = "taxa_anual", nullable = false, precision = 8, scale = 4)
    private BigDecimal taxaAnual;

    @Column(name = "preco_minimo", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoMinimo;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(name = "paga_juros_semestrais", nullable = false)
    private boolean pagaJurosSemestrais;

    @Column(nullable = false)
    private boolean disponivel = true;

    @Column(name = "sincronizado_em")
    private LocalDateTime sincronizadoEm;
}